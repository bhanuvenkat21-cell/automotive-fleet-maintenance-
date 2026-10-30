package com.precisionauto.billing;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {
    private final InvoiceService service;

    public InvoiceController(InvoiceService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TECHNICIAN')")
    public Invoice generate(@Valid @RequestBody Dtos.InvoiceRequest r) { return service.generate(r); }

    @GetMapping("/{id}")
    public Invoice get(@PathVariable Long id) { return service.get(id); }

    @GetMapping("/record/{recordId}")
    public Invoice byRecord(@PathVariable Long recordId) { return service.getByRecord(recordId); }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public Invoice pay(@PathVariable Long id) { return service.markPaid(id); }
}
