package com.precisionauto.billing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceCalculatorTest {
    private static void eq(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @Test
    void computesLabourPartsAndTax() {
        var parts = List.of(new Dtos.PartItem("Oil filter", 1, new BigDecimal("500")),
                            new Dtos.PartItem("Spark plug", 2, new BigDecimal("250")));
        var r = InvoiceCalculator.compute(new BigDecimal("2"), new BigDecimal("800"), parts, new BigDecimal("0.18"));
        assertEquals(3, r.lines().size());
        eq("2600.00", r.subtotal());   // 1600 labour + 500 + 500
        eq("468.00", r.tax());
        eq("3068.00", r.total());
    }

    @Test
    void labourOnlyWhenNoParts() {
        var r = InvoiceCalculator.compute(new BigDecimal("1.5"), new BigDecimal("800"), null, new BigDecimal("0.18"));
        assertEquals(1, r.lines().size());
        eq("1200.00", r.subtotal());
        eq("1416.00", r.total());
    }

    @Test
    void roundsTaxHalfUp() {
        var r = InvoiceCalculator.compute(new BigDecimal("0.5"), new BigDecimal("0.03"), null, new BigDecimal("0.18"));
        eq("0.02", r.subtotal());   // 0.015 -> 0.02
        eq("0.00", r.tax());
    }
}
