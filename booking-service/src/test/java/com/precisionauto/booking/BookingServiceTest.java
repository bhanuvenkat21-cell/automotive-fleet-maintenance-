package com.precisionauto.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock AppointmentRepository repo;
    @Mock RecordClient recordClient;

    BookingService service() { return new BookingService(repo, recordClient, 6); }

    Dtos.CreateBookingRequest req(int bay) {
        return new Dtos.CreateBookingRequest("ap09xy1234", "Alice", "Oil change", bay,
                LocalDateTime.now().plusDays(1), 60);
    }

    @Test
    void schedulesWhenBayFree_andNormalisesRegistration() {
        when(repo.hasOverlap(anyInt(), any(), any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        Appointment a = service().schedule(req(2), "alice");
        assertEquals("AP09XY1234", a.getVehicleReg());
        assertEquals(a.getStartTime().plusMinutes(60), a.getEndTime());
        assertEquals("alice", a.getCreatedBy());
    }

    @Test
    void rejectsCollisionWith409() {
        when(repo.hasOverlap(anyInt(), any(), any(), any())).thenReturn(true);
        var ex = assertThrows(ResponseStatusException.class, () -> service().schedule(req(2), "alice"));
        assertEquals(409, ex.getStatusCode().value());
        verify(repo, never()).save(any());
    }

    @Test
    void rejectsUnknownBay() {
        var ex = assertThrows(ResponseStatusException.class, () -> service().schedule(req(9), "alice"));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void completeCallsRecordServiceAndStoresInvoiceReference() {
        Appointment a = new Appointment(); a.setId(5L); a.setVehicleReg("X"); a.setCustomerName("A");
        when(repo.findById(5L)).thenReturn(Optional.of(a));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(recordClient.createRecord(any())).thenReturn(new Dtos.RecordResponse(11L, 22L, "INV-1", new BigDecimal("100")));

        Appointment done = service().complete(5L,
                new Dtos.CompleteRequest("Changed oil", new BigDecimal("1.5"), List.of()), "tech1");

        assertEquals(AppointmentStatus.COMPLETED, done.getStatus());
        assertEquals(11L, done.getRecordId());
        assertEquals("INV-1", done.getInvoiceNumber());
    }

    @Test
    void completeStaysScheduledWhenDownstreamFails() {
        Appointment a = new Appointment(); a.setId(5L);
        when(repo.findById(5L)).thenReturn(Optional.of(a));
        when(recordClient.createRecord(any())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY));
        assertThrows(ResponseStatusException.class, () -> service().complete(5L,
                new Dtos.CompleteRequest("x", new BigDecimal("1"), null), "tech1"));
        assertEquals(AppointmentStatus.SCHEDULED, a.getStatus());
        verify(repo, never()).save(any());
    }
}
