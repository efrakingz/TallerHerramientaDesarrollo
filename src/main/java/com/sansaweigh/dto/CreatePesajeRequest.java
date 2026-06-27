package com.sansaweigh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreatePesajeRequest {
    @NotBlank(message = "scaleId is required")
    private String scaleId;

    @NotBlank(message = "packageId is required")
    private String packageId;

    @Positive(message = "weightInKg must be positive")
    private double weightInKg;
}
