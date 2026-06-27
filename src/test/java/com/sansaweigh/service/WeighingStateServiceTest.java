package com.sansaweigh.service;

import com.sansaweigh.domain.WeighingState;
import com.sansaweigh.exception.IllegalWeighingStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class WeighingStateServiceTest {

    private WeighingStateService service;

    @BeforeEach
    void setUp() {
        service = new WeighingStateService();
    }

    // ─── Valid transitions ────────────────────────────────────────────────────

    @Test
    @DisplayName("INGRESADO → PESADO is valid")
    void testValidTransition_IngresadoPesado() {
        assertThatNoException().isThrownBy(() ->
                service.validateTransition(WeighingState.INGRESADO, WeighingState.PESADO));
    }

    @Test
    @DisplayName("PESADO → APROBADO is valid")
    void testValidTransition_PesadoAprobado() {
        assertThatNoException().isThrownBy(() ->
                service.validateTransition(WeighingState.PESADO, WeighingState.APROBADO));
    }

    @Test
    @DisplayName("PESADO → RECHAZADO is valid")
    void testValidTransition_PesadoRechazado() {
        assertThatNoException().isThrownBy(() ->
                service.validateTransition(WeighingState.PESADO, WeighingState.RECHAZADO));
    }

    @Test
    @DisplayName("APROBADO → DESPACHADO is valid")
    void testValidTransition_AprobadoDespachado() {
        assertThatNoException().isThrownBy(() ->
                service.validateTransition(WeighingState.APROBADO, WeighingState.DESPACHADO));
    }

    @Test
    @DisplayName("RECHAZADO → DESPACHADO is valid")
    void testValidTransition_RechazadoDespachado() {
        assertThatNoException().isThrownBy(() ->
                service.validateTransition(WeighingState.RECHAZADO, WeighingState.DESPACHADO));
    }

    // ─── Invalid transitions ─────────────────────────────────────────────────

    @Test
    @DisplayName("INGRESADO → APROBADO is invalid")
    void testInvalidTransition_IngresadoAprobado() {
        assertThatThrownBy(() ->
                service.validateTransition(WeighingState.INGRESADO, WeighingState.APROBADO))
                .isInstanceOf(IllegalWeighingStateException.class)
                .hasMessageContaining("INGRESADO")
                .hasMessageContaining("APROBADO");
    }

    @Test
    @DisplayName("INGRESADO → RECHAZADO is invalid")
    void testInvalidTransition_IngresadoRechazado() {
        assertThatThrownBy(() ->
                service.validateTransition(WeighingState.INGRESADO, WeighingState.RECHAZADO))
                .isInstanceOf(IllegalWeighingStateException.class);
    }

    @Test
    @DisplayName("INGRESADO → DESPACHADO is invalid")
    void testInvalidTransition_IngresadoDespachado() {
        assertThatThrownBy(() ->
                service.validateTransition(WeighingState.INGRESADO, WeighingState.DESPACHADO))
                .isInstanceOf(IllegalWeighingStateException.class);
    }

    @Test
    @DisplayName("PESADO → INGRESADO is invalid")
    void testInvalidTransition_PesadoIngresado() {
        assertThatThrownBy(() ->
                service.validateTransition(WeighingState.PESADO, WeighingState.INGRESADO))
                .isInstanceOf(IllegalWeighingStateException.class);
    }

    @Test
    @DisplayName("PESADO → DESPACHADO is invalid")
    void testInvalidTransition_PesadoDespachado() {
        assertThatThrownBy(() ->
                service.validateTransition(WeighingState.PESADO, WeighingState.DESPACHADO))
                .isInstanceOf(IllegalWeighingStateException.class);
    }

    @Test
    @DisplayName("APROBADO → RECHAZADO is invalid")
    void testInvalidTransition_AprobadoRechazado() {
        assertThatThrownBy(() ->
                service.validateTransition(WeighingState.APROBADO, WeighingState.RECHAZADO))
                .isInstanceOf(IllegalWeighingStateException.class);
    }

    @Test
    @DisplayName("APROBADO → INGRESADO is invalid")
    void testInvalidTransition_AprobadoIngresado() {
        assertThatThrownBy(() ->
                service.validateTransition(WeighingState.APROBADO, WeighingState.INGRESADO))
                .isInstanceOf(IllegalWeighingStateException.class);
    }

    @Test
    @DisplayName("RECHAZADO → APROBADO is invalid")
    void testInvalidTransition_RechazadoAprobado() {
        assertThatThrownBy(() ->
                service.validateTransition(WeighingState.RECHAZADO, WeighingState.APROBADO))
                .isInstanceOf(IllegalWeighingStateException.class);
    }

    @Test
    @DisplayName("RECHAZADO → INGRESADO is invalid")
    void testInvalidTransition_RechazadoIngresado() {
        assertThatThrownBy(() ->
                service.validateTransition(WeighingState.RECHAZADO, WeighingState.INGRESADO))
                .isInstanceOf(IllegalWeighingStateException.class);
    }

    @Test
    @DisplayName("DESPACHADO → INGRESADO is invalid (terminal state)")
    void testInvalidTransition_DespachodaIngresado() {
        assertThatThrownBy(() ->
                service.validateTransition(WeighingState.DESPACHADO, WeighingState.INGRESADO))
                .isInstanceOf(IllegalWeighingStateException.class);
    }

    @Test
    @DisplayName("DESPACHADO → PESADO is invalid (terminal state)")
    void testInvalidTransition_DespachadoPesado() {
        assertThatThrownBy(() ->
                service.validateTransition(WeighingState.DESPACHADO, WeighingState.PESADO))
                .isInstanceOf(IllegalWeighingStateException.class);
    }

    @Test
    @DisplayName("DESPACHADO → APROBADO is invalid (terminal state)")
    void testInvalidTransition_DespachadoAprobado() {
        assertThatThrownBy(() ->
                service.validateTransition(WeighingState.DESPACHADO, WeighingState.APROBADO))
                .isInstanceOf(IllegalWeighingStateException.class);
    }

    @Test
    @DisplayName("Exception message contains both from and to states")
    void testExceptionMessageFormat() {
        assertThatThrownBy(() ->
                service.validateTransition(WeighingState.DESPACHADO, WeighingState.RECHAZADO))
                .isInstanceOf(IllegalWeighingStateException.class)
                .hasMessageContaining("DESPACHADO")
                .hasMessageContaining("RECHAZADO");
    }
}
