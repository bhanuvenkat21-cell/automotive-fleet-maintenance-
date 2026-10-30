package com.precisionauto.record;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordServiceTest {
    @Mock ServiceRecordRepository repo;
    @Mock BillingClient billing;

    Dtos.CreateRecordRequest req() {
        return new Dtos.CreateRecordRequest(1L, "ka01ab1234", "Alice", "Brake pads", "tech1",
                new BigDecimal("2"), List.of(new Dtos.PartItem("Pads", 2, new BigDecimal("900"))));
    }

    @Test
    void persistsRecordThenCallsBillingAndStoresInvoiceReference() {
        when(repo.findByAppointmentId(1L)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> { ServiceRecord r = i.getArgument(0); if (r.getId() == null) r.setId(10L); return r; });
        when(billing.generate(any())).thenReturn(new Dtos.InvoiceResponse(99L, "INV-X", new BigDecimal("3540.00")));

        ServiceRecord rec = new RecordService(repo, billing).create(req());

        assertEquals("KA01AB1234", rec.getVehicleReg());
        assertEquals(1, rec.getParts().size());
        assertEquals(99L, rec.getInvoiceId());
        assertEquals("INV-X", rec.getInvoiceNumber());
    }

    @Test
    void retryDoesNotDuplicateWhenInvoiceAlreadyLinked() {
        ServiceRecord existing = new ServiceRecord();
        existing.setId(10L); existing.setInvoiceId(99L);
        when(repo.findByAppointmentId(1L)).thenReturn(Optional.of(existing));

        ServiceRecord rec = new RecordService(repo, billing).create(req());

        assertSame(existing, rec);
        verify(billing, never()).generate(any());
        verify(repo, never()).save(any());
    }

    @Test
    void retryReattemptsBillingWhenPreviousCallFailed() {
        ServiceRecord existing = new ServiceRecord();
        existing.setId(10L); existing.setCustomerName("Alice"); existing.setVehicleReg("KA01AB1234");
        existing.setLaborHours(BigDecimal.ONE);
        when(repo.findByAppointmentId(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(billing.generate(any())).thenReturn(new Dtos.InvoiceResponse(5L, "INV-Y", BigDecimal.TEN));

        ServiceRecord rec = new RecordService(repo, billing).create(req());

        assertEquals(5L, rec.getInvoiceId());
        verify(billing, times(1)).generate(any());
    }

    @Test
    void historyNormalisesRegistration() {
        new RecordService(repo, billing).history(" ka01ab1234 ");
        verify(repo).findByVehicleRegOrderByCompletedAtDesc("KA01AB1234");
    }
}
