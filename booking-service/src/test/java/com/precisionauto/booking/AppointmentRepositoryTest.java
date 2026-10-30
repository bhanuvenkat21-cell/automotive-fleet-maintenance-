package com.precisionauto.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class AppointmentRepositoryTest {
    @Autowired AppointmentRepository repo;

    static final LocalDateTime T9 = LocalDateTime.of(2030, 1, 1, 9, 0);

    private void save(int bay, AppointmentStatus st, LocalDateTime s, LocalDateTime e) {
        Appointment a = new Appointment();
        a.setBayNumber(bay); a.setStatus(st); a.setStartTime(s); a.setEndTime(e); a.setCreatedBy("u");
        repo.saveAndFlush(a);
    }

    @Test
    void detectsPartialAndFullOverlap() {
        save(1, AppointmentStatus.SCHEDULED, T9, T9.plusHours(2));                       // 09:00-11:00
        assertTrue(repo.hasOverlap(1, AppointmentStatus.CANCELLED, T9.plusHours(1), T9.plusHours(3)));  // partial
        assertTrue(repo.hasOverlap(1, AppointmentStatus.CANCELLED, T9.plusMinutes(30), T9.plusHours(1))); // inside
        assertTrue(repo.hasOverlap(1, AppointmentStatus.CANCELLED, T9.minusHours(1), T9.plusHours(5)));  // encloses
    }

    @Test
    void backToBackSlotsAndOtherBaysAreAllowed() {
        save(1, AppointmentStatus.SCHEDULED, T9, T9.plusHours(2));
        assertFalse(repo.hasOverlap(1, AppointmentStatus.CANCELLED, T9.plusHours(2), T9.plusHours(3)));
        assertFalse(repo.hasOverlap(1, AppointmentStatus.CANCELLED, T9.minusHours(1), T9));
        assertFalse(repo.hasOverlap(2, AppointmentStatus.CANCELLED, T9, T9.plusHours(2)));
    }

    @Test
    void cancelledAppointmentsFreeTheBay() {
        save(1, AppointmentStatus.CANCELLED, T9, T9.plusHours(2));
        assertFalse(repo.hasOverlap(1, AppointmentStatus.CANCELLED, T9, T9.plusHours(2)));
    }
}
