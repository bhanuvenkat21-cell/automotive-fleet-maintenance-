package com.precisionauto.record;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/records")
public class RecordController {
    private final RecordService service;

    public RecordController(RecordService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ServiceRecord create(@Valid @RequestBody Dtos.CreateRecordRequest r) { return service.create(r); }

    @GetMapping("/vehicle/{reg}")
    public List<ServiceRecord> history(@PathVariable String reg) { return service.history(reg); }

    @GetMapping("/{id}")
    public ServiceRecord get(@PathVariable Long id) { return service.get(id); }
}
