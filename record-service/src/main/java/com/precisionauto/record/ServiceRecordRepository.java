package com.precisionauto.record;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {
    Optional<ServiceRecord> findByAppointmentId(Long appointmentId);
    List<ServiceRecord> findByVehicleRegOrderByCompletedAtDesc(String vehicleReg);
}
