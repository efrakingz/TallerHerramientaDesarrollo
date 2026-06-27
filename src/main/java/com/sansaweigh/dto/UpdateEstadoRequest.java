package com.sansaweigh.dto;

import com.sansaweigh.domain.WeighingState;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateEstadoRequest {
    @NotNull(message = "newState is required")
    private WeighingState newState;
}
