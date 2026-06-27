package com.sansaweigh.service;

import com.sansaweigh.domain.*;
import com.sansaweigh.dto.*;
import com.sansaweigh.exception.ResourceNotFoundException;
import com.sansaweigh.integration.ExternalScaleClient;
import com.sansaweigh.repository.RegistroPesajeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PesajeService {

    private final RegistroPesajeRepository repository;
    private final WeighingBusinessRulesService rulesService;
    private final WeighingStateService stateService;
    private final ExternalScaleClient externalScaleClient;

    /**
     * Creates a new weighing record:
     * 1. Converts weightInKg → weightInSansas
     * 2. Classifies weight category
     * 3. Applies business rules (night restriction, prime-scale rule)
     * 4. Fetches scale specifications (cached + retry)
     * 5. Persists the record in MongoDB with initial state INGRESADO
     */
    public PesajeResponse createPesaje(CreatePesajeRequest request) {
        LocalDateTime now = LocalDateTime.now();

        double weightInSansas = rulesService.convertKgToSansas(request.getWeightInKg());
        WeightCategory category = rulesService.classify(weightInSansas);

        // Validate business rules
        rulesService.validateNightRestriction(category, now);
        rulesService.validatePrimeScaleRule(request.getScaleId(), category, now);

        // Fetch scale specifications (with cache and retry)
        ScaleSpecification scaleSpec = externalScaleClient.getScaleSpecifications(request.getScaleId());
        log.info("Using scale: {}", scaleSpec);

        StateTransition initialTransition = StateTransition.builder()
                .fromState(null)
                .toState(WeighingState.INGRESADO)
                .timestamp(Instant.now())
                .build();

        RegistroPesaje pesaje = RegistroPesaje.builder()
                .scaleId(request.getScaleId())
                .packageId(request.getPackageId())
                .weightInKg(request.getWeightInKg())
                .weightInSansas(weightInSansas)
                .weightCategory(category)
                .currentState(WeighingState.INGRESADO)
                .stateHistory(new ArrayList<>(List.of(initialTransition)))
                .build();

        RegistroPesaje saved = repository.save(pesaje);
        return toResponse(saved);
    }

    /**
     * Transitions a weighing record to a new state.
     * Validates the transition via the state machine before persisting.
     */
    public PesajeResponse updateEstado(String id, UpdateEstadoRequest request) {
        RegistroPesaje pesaje = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pesaje not found: " + id));

        stateService.validateTransition(pesaje.getCurrentState(), request.getNewState());

        StateTransition transition = StateTransition.builder()
                .fromState(pesaje.getCurrentState())
                .toState(request.getNewState())
                .timestamp(Instant.now())
                .build();

        pesaje.getStateHistory().add(transition);
        pesaje.setCurrentState(request.getNewState());

        RegistroPesaje saved = repository.save(pesaje);
        return toResponse(saved);
    }

    /**
     * Returns all weighing records created on the given date (server timezone).
     */
    public List<PesajeResponse> getPesajesByFecha(LocalDate fecha) {
        Instant start = fecha.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = fecha.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return repository.findByCreatedAtBetween(start, end)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PesajeResponse toResponse(RegistroPesaje pesaje) {
        return PesajeResponse.builder()
                .id(pesaje.getId())
                .scaleId(pesaje.getScaleId())
                .packageId(pesaje.getPackageId())
                .weightInKg(pesaje.getWeightInKg())
                .weightInSansas(pesaje.getWeightInSansas())
                .weightCategory(pesaje.getWeightCategory())
                .currentState(pesaje.getCurrentState())
                .stateHistory(pesaje.getStateHistory())
                .createdAt(pesaje.getCreatedAt())
                .updatedAt(pesaje.getUpdatedAt())
                .build();
    }
}
