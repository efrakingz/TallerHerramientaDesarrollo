package com.sansaweigh.service;

import com.sansaweigh.domain.WeighingState;
import com.sansaweigh.exception.IllegalWeighingStateException;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class WeighingStateService {

    /**
     * Valid state machine transitions:
     * INGRESADO → PESADO
     * PESADO    → APROBADO | RECHAZADO
     * APROBADO  → DESPACHADO
     * RECHAZADO → DESPACHADO
     * DESPACHADO → (terminal — no further transitions)
     */
    private static final Map<WeighingState, Set<WeighingState>> VALID_TRANSITIONS = Map.of(
            WeighingState.INGRESADO,  EnumSet.of(WeighingState.PESADO),
            WeighingState.PESADO,     EnumSet.of(WeighingState.APROBADO, WeighingState.RECHAZADO),
            WeighingState.APROBADO,   EnumSet.of(WeighingState.DESPACHADO),
            WeighingState.RECHAZADO,  EnumSet.of(WeighingState.DESPACHADO),
            WeighingState.DESPACHADO, EnumSet.noneOf(WeighingState.class)
    );

    /**
     * Validates that the requested state transition is allowed.
     * Throws IllegalWeighingStateException (HTTP 400) if the transition is illegal.
     */
    public void validateTransition(WeighingState current, WeighingState next) {
        Set<WeighingState> allowed = VALID_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(WeighingState.class));
        if (!allowed.contains(next)) {
            throw new IllegalWeighingStateException(
                    String.format("Invalid state transition from %s to %s", current, next));
        }
    }
}
