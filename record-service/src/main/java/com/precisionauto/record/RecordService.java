package com.precisionauto.record;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RecordService {
    private final ServiceRecordRepository repo;
    private final BillingClient billing;

    public RecordService(ServiceRecordRepository repo, BillingClient billing) {
        this.repo = repo; this.billing = billing;
    }

    /**
     * Idempotent on appointmentId. The record is persisted first (so history is never lost), then billing is
     * called. If billing fails the record stays with no invoice and the next retry only re-attempts billing.
     */
    public ServiceRecord create(Dtos.CreateRecordRequest r) {
        ServiceRecord rec = repo.findByAppointmentId(r.appointmentId()).orElseGet(() -> {
            ServiceRecord n = new ServiceRecord();
            n.setAppointmentId(r.appointmentId());
            n.setVehicleReg(r.vehicleReg().trim().toUpperCase());
            n.setCustomerName(r.customerName());
            n.setWorkDescription(r.workDescription());
            n.setTechnician(r.technician());
            n.setLaborHours(r.laborHours());
            if (r.parts() != null)
                r.parts().forEach(p -> n.getParts().add(new PartUsed(p.name(), p.quantity(), p.unitPrice())));
            return repo.save(n);
        });

        if (rec.getInvoiceId() == null) {
            List<Dtos.PartItem> items = rec.getParts().stream()
                    .map(p -> new Dtos.PartItem(p.name(), p.quantity(), p.unitPrice())).toList();
            Dtos.InvoiceResponse inv = billing.generate(new Dtos.InvoiceRequest(
                    rec.getId(), rec.getCustomerName(), rec.getVehicleReg(), rec.getLaborHours(), items));
            rec.setInvoiceId(inv.id());
            rec.setInvoiceNumber(inv.invoiceNumber());
            rec.setInvoiceTotal(inv.total());
            rec = repo.save(rec);
        }
        return rec;
    }

    public List<ServiceRecord> history(String vehicleReg) {
        return repo.findByVehicleRegOrderByCompletedAtDesc(vehicleReg.trim().toUpperCase());
    }

    public ServiceRecord get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
