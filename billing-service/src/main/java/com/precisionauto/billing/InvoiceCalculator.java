package com.precisionauto.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Pure pricing logic: labour + parts, then tax. Kept static so it is trivially unit-testable. */
public final class InvoiceCalculator {
    private InvoiceCalculator() {}

    public record Result(List<InvoiceLine> lines, BigDecimal subtotal, BigDecimal tax, BigDecimal total) {}

    public static Result compute(BigDecimal laborHours, BigDecimal laborRate,
                                 List<Dtos.PartItem> parts, BigDecimal taxRate) {
        List<InvoiceLine> lines = new ArrayList<>();
        lines.add(line("Labour (hours)", laborHours, laborRate));
        if (parts != null)
            for (Dtos.PartItem p : parts)
                lines.add(line(p.name(), BigDecimal.valueOf(p.quantity()), p.unitPrice()));

        BigDecimal subtotal = lines.stream().map(InvoiceLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        return new Result(lines, subtotal, tax, subtotal.add(tax));
    }

    private static InvoiceLine line(String desc, BigDecimal qty, BigDecimal unit) {
        return new InvoiceLine(desc, qty, unit, qty.multiply(unit).setScale(2, RoundingMode.HALF_UP));
    }
}
