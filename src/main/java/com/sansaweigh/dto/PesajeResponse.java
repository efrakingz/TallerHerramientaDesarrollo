package com.sansaweigh.dto;

import com.sansaweigh.domain.StateTransition;
import com.sansaweigh.domain.WeightCategory;
import com.sansaweigh.domain.WeighingState;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class PesajeResponse {
    private String id;
    private String scaleId;
    private String packageId;
    private double weightInSansas;
    private double weightInKg;
    private WeightCategory weightCategory;
    private WeighingState currentState;
    private List<StateTransition> stateHistory;
    private Instant createdAt;
    private Instant updatedAt;
}
