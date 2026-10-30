package com.precisionauto.record;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record PartItem(@NotBlank String name, @Min(1) int quantity, @NotNull @DecimalMin("0.0") BigDecimal unitPrice) {}

    public record CreateRecordRequest(@NotNull Long appointmentId, @NotBlank String vehicleReg,
                                      @NotBlank String customerName, @NotBlank String workDescription,
                                      @NotBlank String technician, @NotNull @DecimalMin("0.0") BigDecimal laborHours,
                                      @Valid List<PartItem> parts) {}

    // --- payloads exchanged with billing-service ---
    public record InvoiceRequest(Long recordId, String customerName, String vehicleReg,
                                 BigDecimal laborHours, List<PartItem> parts) {}

    public record InvoiceResponse(Long id, String invoiceNumber, BigDecimal total) {}
}
