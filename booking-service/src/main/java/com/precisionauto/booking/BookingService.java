package com.precisionauto.booking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class BookingService {
    private final AppointmentRepository repo;
    private final RecordClient recordClient;
    private final int bays;

    public BookingService(AppointmentRepository repo, RecordClient recordClient,
                          @Value("${booking.bays:6}") int bays) {
        this.repo = repo; this.recordClient = recordClient; this.bays = bays;
    }

    /**
     * SERIALIZABLE isolation makes the check-then-insert atomic across service instances; a losing concurrent
     * transaction fails with a serialization error that the controller advice maps to HTTP 409.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Appointment schedule(Dtos.CreateBookingRequest r, String user) {
        if (r.bayNumber() > bays)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown bay. Valid bays: 1-" + bays);
        LocalDateTime end = r.startTime().plusMinutes(r.durationMinutes());
        if (repo.hasOverlap(r.bayNumber(), AppointmentStatus.CANCELLED, r.startTime(), end))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Bay " + r.bayNumber() + " is already booked for the requested time window");
        Appointment a = new Appointment();
        a.setVehicleReg(r.vehicleReg().trim().toUpperCase());
        a.setCustomerName(r.customerName());
        a.setServiceType(r.serviceType());
        a.setBayNumber(r.bayNumber());
        a.setStartTime(r.startTime());
        a.setEndTime(end);
        a.setCreatedBy(user);
        return repo.save(a);
    }

    public List<Integer> freeBays(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "end must be after start");
        return IntStream.rangeClosed(1, bays)
                .filter(b -> !repo.hasOverlap(b, AppointmentStatus.CANCELLED, start, end)).boxed().toList();
    }

    public Appointment get(Long id, String user, boolean technician) {
        Appointment a = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!technician && !a.getCreatedBy().equals(user)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return a;
    }

    public List<Appointment> list(String user, boolean technician) {
        return technician ? repo.findAllByOrderByStartTimeAsc() : repo.findByCreatedByOrderByStartTimeAsc(user);
    }

    @Transactional
    public Appointment cancel(Long id, String user, boolean technician) {
        Appointment a = get(id, user, technician);
        if (a.getStatus() != AppointmentStatus.SCHEDULED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only SCHEDULED appointments can be cancelled");
        a.setStatus(AppointmentStatus.CANCELLED);
        return a;
    }

    /**
     * Completion pipeline: Booking -> Record -> Billing. Record-service is idempotent on appointmentId, so a
     * retry after a partial failure never duplicates the record or the invoice.
     */
    public Appointment complete(Long id, Dtos.CompleteRequest r, String technician) {
        Appointment a = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (a.getStatus() == AppointmentStatus.CANCELLED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Appointment is cancelled");
        if (a.getStatus() == AppointmentStatus.COMPLETED) return a;

        Dtos.RecordResponse rec = recordClient.createRecord(new Dtos.CreateRecordRequest(
                a.getId(), a.getVehicleReg(), a.getCustomerName(), r.workDescription(), technician,
                r.laborHours(), r.parts() == null ? List.of() : r.parts()));

        a.setStatus(AppointmentStatus.COMPLETED);
        a.setRecordId(rec.id());
        a.setInvoiceId(rec.invoiceId());
        a.setInvoiceNumber(rec.invoiceNumber());
        return repo.save(a);
    }
}
