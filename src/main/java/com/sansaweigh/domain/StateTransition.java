package com.sansaweigh.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateTransition {
    private WeighingState fromState;
    private WeighingState toState;
    private Instant timestamp;
}
