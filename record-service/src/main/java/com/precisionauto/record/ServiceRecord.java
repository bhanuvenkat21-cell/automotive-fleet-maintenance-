package com.precisionauto.record;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_records", indexes = @Index(name = "idx_record_vehicle", columnList = "vehicleReg"))
@Getter @Setter @NoArgsConstructor
public class ServiceRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private Long appointmentId;
    private String vehicleReg;
    private String customerName;
    @Column(length = 2000)
    private String workDescription;
    private String technician;
    private BigDecimal laborHours;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "record_parts", joinColumns = @JoinColumn(name = "record_id"))
    private List<PartUsed> parts = new ArrayList<>();
    private LocalDateTime completedAt = LocalDateTime.now();
    private Long invoiceId;
    private String invoiceNumber;
    private BigDecimal invoiceTotal;
}
