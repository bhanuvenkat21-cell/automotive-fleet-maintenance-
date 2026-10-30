package com.precisionauto.record;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public record PartUsed(String name, int quantity, BigDecimal unitPrice) {}
