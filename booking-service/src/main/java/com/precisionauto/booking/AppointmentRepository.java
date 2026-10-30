package com.precisionauto.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /** Two intervals overlap iff existing.start < new.end AND existing.end > new.start (back-to-back slots are allowed). */
    @Query("""
           select count(a) > 0 from Appointment a
           where a.bayNumber = :bay and a.status <> :cancelled
             and a.startTime < :end and a.endTime > :start
           """)
    boolean hasOverlap(@Param("bay") int bay, @Param("cancelled") AppointmentStatus cancelled,
                       @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<Appointment> findByCreatedByOrderByStartTimeAsc(String createdBy);
    List<Appointment> findAllByOrderByStartTimeAsc();
}
