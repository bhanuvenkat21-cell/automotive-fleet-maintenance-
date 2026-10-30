package com.precisionauto.booking;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    private final BookingService service;

    public BookingController(BookingService service) { this.service = service; }

    private static boolean tech(Authentication a) {
        return a.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ROLE_TECHNICIAN"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Appointment create(@Valid @RequestBody Dtos.CreateBookingRequest r, Authentication auth) {
        return service.schedule(r, auth.getName());
    }

    @GetMapping("/availability")
    public List<Integer> availability(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return service.freeBays(start, end);
    }

    @GetMapping
    public List<Appointment> list(Authentication auth) { return service.list(auth.getName(), tech(auth)); }

    @GetMapping("/{id}")
    public Appointment get(@PathVariable Long id, Authentication auth) { return service.get(id, auth.getName(), tech(auth)); }

    @PatchMapping("/{id}/cancel")
    public Appointment cancel(@PathVariable Long id, Authentication auth) { return service.cancel(id, auth.getName(), tech(auth)); }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public Appointment complete(@PathVariable Long id, @Valid @RequestBody Dtos.CompleteRequest r, Authentication auth) {
        return service.complete(id, r, auth.getName());
    }
}
