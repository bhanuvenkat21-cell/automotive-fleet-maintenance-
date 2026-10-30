package com.precisionauto.billing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter @Setter @NoArgsConstructor
public class Invoice {
    public enum Status { ISSUED, PAID }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String invoiceNumber;
    @Column(unique = true, nullable = false)
    private Long recordId;
    private String customerName;
    private String vehicleReg;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "invoice_lines", joinColumns = @JoinColumn(name = "invoice_id"))
    private List<InvoiceLine> lines = new ArrayList<>();
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal total;
    @Enumerated(EnumType.STRING)
    private Status status = Status.ISSUED;
    private LocalDateTime issuedAt = LocalDateTime.now();
}
