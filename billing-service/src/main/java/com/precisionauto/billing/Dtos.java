package com.precisionauto.billing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record PartItem(@NotBlank String name, @Min(1) int quantity, @NotNull @DecimalMin("0.0") BigDecimal unitPrice) {}

    public record InvoiceRequest(@NotNull Long recordId, @NotBlank String customerName, @NotBlank String vehicleReg,
                                 @NotNull @DecimalMin("0.0") BigDecimal laborHours, @Valid List<PartItem> parts) {}
}
