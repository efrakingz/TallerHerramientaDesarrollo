package com.sansaweigh.service;

import com.sansaweigh.domain.*;
import com.sansaweigh.dto.*;
import com.sansaweigh.exception.IllegalWeighingStateException;
import com.sansaweigh.exception.ResourceNotFoundException;
import com.sansaweigh.integration.ExternalScaleClient;
import com.sansaweigh.repository.RegistroPesajeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PesajeServiceTest {

    @Mock private RegistroPesajeRepository repository;
    @Mock private WeighingBusinessRulesService rulesService;
    @Mock private WeighingStateService stateService;
    @Mock private ExternalScaleClient externalScaleClient;

    @InjectMocks private PesajeService service;

    private ScaleSpecification mockSpec;

    @BeforeEach
    void setUp() {
        mockSpec = ScaleSpecification.builder()
                .id("101")
                .name("Test Scale")
                .brand("SansaScale-Pro")
                .maxCapacity(150.0)
                .precision(0.01)
                .lastCalibrationOffset(-0.05)
                .build();
    }

    // ─── createPesaje ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createPesaje: success returns response with INGRESADO state")
    void testCreatePesaje_Success() {
        CreatePesajeRequest request = new CreatePesajeRequest();
        request.setScaleId("101");
        request.setPackageId("PKG-001");
        request.setWeightInKg(10.0);

        when(rulesService.convertKgToSansas(10.0)).thenReturn(7.48);
        when(rulesService.classify(7.48)).thenReturn(WeightCategory.LIVIANO);
        when(externalScaleClient.getScaleSpecifications("101")).thenReturn(mockSpec);

        RegistroPesaje saved = RegistroPesaje.builder()
                .id("abc123")
                .scaleId("101")
                .packageId("PKG-001")
                .weightInKg(10.0)
                .weightInSansas(7.48)
                .weightCategory(WeightCategory.LIVIANO)
                .currentState(WeighingState.INGRESADO)
                .stateHistory(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(repository.save(any(RegistroPesaje.class))).thenReturn(saved);

        PesajeResponse response = service.createPesaje(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("abc123");
        assertThat(response.getCurrentState()).isEqualTo(WeighingState.INGRESADO);
        assertThat(response.getWeightCategory()).isEqualTo(WeightCategory.LIVIANO);
        assertThat(response.getScaleId()).isEqualTo("101");
        assertThat(response.getPackageId()).isEqualTo("PKG-001");

        verify(rulesService).validateNightRestriction(eq(WeightCategory.LIVIANO), any());
        verify(rulesService).validatePrimeScaleRule(eq("101"), eq(WeightCategory.LIVIANO), any());
        verify(externalScaleClient).getScaleSpecifications("101");
        verify(repository).save(any(RegistroPesaje.class));
    }

    @Test
    @DisplayName("createPesaje: PESADO category triggers business rules validation")
    void testCreatePesaje_PesadoCategory() {
        CreatePesajeRequest request = new CreatePesajeRequest();
        request.setScaleId("7");
        request.setPackageId("PKG-HEAVY");
        request.setWeightInKg(100.0);

        when(rulesService.convertKgToSansas(100.0)).thenReturn(74.8);
        when(rulesService.classify(74.8)).thenReturn(WeightCategory.PESADO);
        when(externalScaleClient.getScaleSpecifications("7")).thenReturn(mockSpec);

        RegistroPesaje saved = RegistroPesaje.builder()
                .id("xyz789")
                .scaleId("7")
                .packageId("PKG-HEAVY")
                .weightInKg(100.0)
                .weightInSansas(74.8)
                .weightCategory(WeightCategory.PESADO)
                .currentState(WeighingState.INGRESADO)
                .stateHistory(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(repository.save(any(RegistroPesaje.class))).thenReturn(saved);

        PesajeResponse response = service.createPesaje(request);

        assertThat(response.getWeightCategory()).isEqualTo(WeightCategory.PESADO);
        verify(rulesService).validateNightRestriction(eq(WeightCategory.PESADO), any());
        verify(rulesService).validatePrimeScaleRule(eq("7"), eq(WeightCategory.PESADO), any());
    }

    @Test
    @DisplayName("createPesaje: propagates BusinessException from night restriction")
    void testCreatePesaje_NightRestrictionThrows() {
        CreatePesajeRequest request = new CreatePesajeRequest();
        request.setScaleId("1");
        request.setPackageId("PKG-X");
        request.setWeightInKg(80.0);

        when(rulesService.convertKgToSansas(80.0)).thenReturn(59.8);
        when(rulesService.classify(59.8)).thenReturn(WeightCategory.PESADO);
        doThrow(new com.sansaweigh.exception.BusinessException("Night restriction"))
                .when(rulesService).validateNightRestriction(eq(WeightCategory.PESADO), any());

        assertThatThrownBy(() -> service.createPesaje(request))
                .isInstanceOf(com.sansaweigh.exception.BusinessException.class)
                .hasMessageContaining("Night restriction");

        verify(repository, never()).save(any());
    }

    // ─── updateEstado ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateEstado: valid transition updates state and adds history entry")
    void testUpdateEstado_Success() {
        RegistroPesaje existing = RegistroPesaje.builder()
                .id("abc123")
                .scaleId("101")
                .currentState(WeighingState.INGRESADO)
                .stateHistory(new ArrayList<>())
                .build();

        UpdateEstadoRequest request = new UpdateEstadoRequest();
        request.setNewState(WeighingState.PESADO);

        when(repository.findById("abc123")).thenReturn(Optional.of(existing));

        RegistroPesaje updated = RegistroPesaje.builder()
                .id("abc123")
                .scaleId("101")
                .currentState(WeighingState.PESADO)
                .stateHistory(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(repository.save(any(RegistroPesaje.class))).thenReturn(updated);

        PesajeResponse response = service.updateEstado("abc123", request);

        assertThat(response.getCurrentState()).isEqualTo(WeighingState.PESADO);
        verify(stateService).validateTransition(WeighingState.INGRESADO, WeighingState.PESADO);
        verify(repository).save(any(RegistroPesaje.class));
    }

    @Test
    @DisplayName("updateEstado: throws ResourceNotFoundException when id not found")
    void testUpdateEstado_NotFound() {
        when(repository.findById("nonexistent")).thenReturn(Optional.empty());

        UpdateEstadoRequest request = new UpdateEstadoRequest();
        request.setNewState(WeighingState.PESADO);

        assertThatThrownBy(() -> service.updateEstado("nonexistent", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nonexistent");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("updateEstado: propagates IllegalWeighingStateException on invalid transition")
    void testUpdateEstado_InvalidTransition() {
        RegistroPesaje existing = RegistroPesaje.builder()
                .id("abc123")
                .currentState(WeighingState.INGRESADO)
                .stateHistory(new ArrayList<>())
                .build();

        UpdateEstadoRequest request = new UpdateEstadoRequest();
        request.setNewState(WeighingState.APROBADO);

        when(repository.findById("abc123")).thenReturn(Optional.of(existing));
        doThrow(new IllegalWeighingStateException("Invalid"))
                .when(stateService).validateTransition(WeighingState.INGRESADO, WeighingState.APROBADO);

        assertThatThrownBy(() -> service.updateEstado("abc123", request))
                .isInstanceOf(IllegalWeighingStateException.class);

        verify(repository, never()).save(any());
    }

    // ─── getPesajesByFecha ────────────────────────────────────────────────────

    @Test
    @DisplayName("getPesajesByFecha: returns list for given date")
    void testGetPesajesByFecha_ReturnsList() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        RegistroPesaje pesaje = RegistroPesaje.builder()
                .id("abc")
                .scaleId("101")
                .packageId("PKG-001")
                .currentState(WeighingState.INGRESADO)
                .weightCategory(WeightCategory.LIVIANO)
                .stateHistory(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(repository.findByCreatedAtBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(pesaje));

        List<PesajeResponse> result = service.getPesajesByFecha(date);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("abc");
        assertThat(result.get(0).getScaleId()).isEqualTo("101");
    }

    @Test
    @DisplayName("getPesajesByFecha: returns empty list when no records found")
    void testGetPesajesByFecha_Empty() {
        when(repository.findByCreatedAtBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        List<PesajeResponse> result = service.getPesajesByFecha(LocalDate.of(2024, 6, 1));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getPesajesByFecha: maps all fields correctly")
    void testGetPesajesByFecha_MapsFields() {
        Instant now = Instant.now();
        RegistroPesaje pesaje = RegistroPesaje.builder()
                .id("id1")
                .scaleId("5")
                .packageId("PKG-500")
                .weightInKg(20.0)
                .weightInSansas(14.96)
                .weightCategory(WeightCategory.MEDIANO)
                .currentState(WeighingState.PESADO)
                .stateHistory(new ArrayList<>())
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(repository.findByCreatedAtBetween(any(), any())).thenReturn(List.of(pesaje));

        PesajeResponse resp = service.getPesajesByFecha(LocalDate.now()).get(0);

        assertThat(resp.getId()).isEqualTo("id1");
        assertThat(resp.getWeightCategory()).isEqualTo(WeightCategory.MEDIANO);
        assertThat(resp.getCurrentState()).isEqualTo(WeighingState.PESADO);
        assertThat(resp.getWeightInKg()).isEqualTo(20.0);
        assertThat(resp.getWeightInSansas()).isEqualTo(14.96);
        assertThat(resp.getCreatedAt()).isEqualTo(now);
    }
}
