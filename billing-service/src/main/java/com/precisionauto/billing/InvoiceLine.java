package com.precisionauto.billing;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public record InvoiceLine(String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {}
