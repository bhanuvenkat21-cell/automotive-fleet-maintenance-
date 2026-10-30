package com.precisionauto.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record CreateBookingRequest(@NotBlank String vehicleReg, @NotBlank String customerName,
                                       @NotBlank String serviceType, @NotNull @Min(1) Integer bayNumber,
                                       @NotNull @Future LocalDateTime startTime,
                                       @NotNull @Min(15) @Max(720) Integer durationMinutes) {}

    public record PartItem(@NotBlank String name, @Min(1) int quantity,
                           @NotNull @DecimalMin("0.0") BigDecimal unitPrice) {}

    public record CompleteRequest(@NotBlank String workDescription,
                                  @NotNull @DecimalMin("0.5") BigDecimal laborHours,
                                  @Valid List<PartItem> parts) {}

    // --- payloads exchanged with record-service ---
    public record CreateRecordRequest(Long appointmentId, String vehicleReg, String customerName,
                                      String workDescription, String technician,
                                      BigDecimal laborHours, List<PartItem> parts) {}

    public record RecordResponse(Long id, Long invoiceId, String invoiceNumber, BigDecimal invoiceTotal) {}
}
