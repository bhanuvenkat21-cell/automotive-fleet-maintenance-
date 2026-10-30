package com.precisionauto.billing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class InvoiceService {
    private final InvoiceRepository repo;
    private final BigDecimal laborRate;
    private final BigDecimal taxRate;

    public InvoiceService(InvoiceRepository repo,
                          @Value("${billing.labor-rate:800.00}") BigDecimal laborRate,
                          @Value("${billing.tax-rate:0.18}") BigDecimal taxRate) {
        this.repo = repo; this.laborRate = laborRate; this.taxRate = taxRate;
    }

    /** Idempotent on recordId: replays return the existing invoice instead of billing twice. */
    @Transactional
    public Invoice generate(Dtos.InvoiceRequest r) {
        return repo.findByRecordId(r.recordId()).orElseGet(() -> {
            var calc = InvoiceCalculator.compute(r.laborHours(), laborRate, r.parts(), taxRate);
            Invoice inv = new Invoice();
            inv.setRecordId(r.recordId());
            inv.setCustomerName(r.customerName());
            inv.setVehicleReg(r.vehicleReg());
            inv.setLines(calc.lines());
            inv.setSubtotal(calc.subtotal());
            inv.setTaxAmount(calc.tax());
            inv.setTotal(calc.total());
            inv = repo.save(inv);
            inv.setInvoiceNumber("INV-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                    + "-" + String.format("%05d", inv.getId()));
            return inv;
        });
    }

    public Invoice get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Invoice getByRecord(Long recordId) {
        return repo.findByRecordId(recordId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Invoice markPaid(Long id) {
        Invoice inv = get(id);
        inv.setStatus(Invoice.Status.PAID);
        return inv;
    }
}
