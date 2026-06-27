package com.sansaweigh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sansaweigh.domain.WeightCategory;
import com.sansaweigh.domain.WeighingState;
import com.sansaweigh.dto.CreatePesajeRequest;
import com.sansaweigh.dto.PesajeResponse;
import com.sansaweigh.dto.UpdateEstadoRequest;
import com.sansaweigh.exception.BusinessException;
import com.sansaweigh.exception.IllegalWeighingStateException;
import com.sansaweigh.exception.ResourceNotFoundException;
import com.sansaweigh.service.PesajeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PesajeController.class)
class PesajeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PesajeService pesajeService;

    // ─── POST /api/pesajes ────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/pesajes → 201 with valid request")
    void testCreatePesaje_Returns201() throws Exception {
        CreatePesajeRequest req = new CreatePesajeRequest();
        req.setScaleId("101");
        req.setPackageId("PKG-001");
        req.setWeightInKg(10.0);

        PesajeResponse resp = PesajeResponse.builder()
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

        when(pesajeService.createPesaje(any(CreatePesajeRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/pesajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("abc123"))
                .andExpect(jsonPath("$.currentState").value("INGRESADO"))
                .andExpect(jsonPath("$.weightCategory").value("LIVIANO"));
    }

    @Test
    @DisplayName("POST /api/pesajes → 400 when scaleId is blank")
    void testCreatePesaje_BlankScaleId_Returns400() throws Exception {
        CreatePesajeRequest req = new CreatePesajeRequest();
        req.setScaleId("");
        req.setPackageId("PKG-001");
        req.setWeightInKg(10.0);

        mockMvc.perform(post("/api/pesajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/pesajes → 400 when packageId is blank")
    void testCreatePesaje_BlankPackageId_Returns400() throws Exception {
        CreatePesajeRequest req = new CreatePesajeRequest();
        req.setScaleId("101");
        req.setPackageId("");
        req.setWeightInKg(5.0);

        mockMvc.perform(post("/api/pesajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/pesajes → 400 when weightInKg is negative")
    void testCreatePesaje_NegativeWeight_Returns400() throws Exception {
        CreatePesajeRequest req = new CreatePesajeRequest();
        req.setScaleId("101");
        req.setPackageId("PKG-001");
        req.setWeightInKg(-5.0);

        mockMvc.perform(post("/api/pesajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/pesajes → 422 when BusinessException thrown")
    void testCreatePesaje_BusinessException_Returns422() throws Exception {
        CreatePesajeRequest req = new CreatePesajeRequest();
        req.setScaleId("7");
        req.setPackageId("PKG-HEAVY");
        req.setWeightInKg(100.0);

        when(pesajeService.createPesaje(any())).thenThrow(
                new BusinessException("PESADO packages cannot be processed between 20:00 and 06:00"));

        mockMvc.perform(post("/api/pesajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        "PESADO packages cannot be processed between 20:00 and 06:00"));
    }

    // ─── PUT /api/pesajes/{id}/estado ─────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/pesajes/{id}/estado → 200 on valid transition")
    void testUpdateEstado_Returns200() throws Exception {
        UpdateEstadoRequest req = new UpdateEstadoRequest();
        req.setNewState(WeighingState.PESADO);

        PesajeResponse resp = PesajeResponse.builder()
                .id("abc123")
                .scaleId("101")
                .currentState(WeighingState.PESADO)
                .stateHistory(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(pesajeService.updateEstado(eq("abc123"), any(UpdateEstadoRequest.class))).thenReturn(resp);

        mockMvc.perform(put("/api/pesajes/abc123/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentState").value("PESADO"));
    }

    @Test
    @DisplayName("PUT /api/pesajes/{id}/estado → 400 on invalid state transition")
    void testUpdateEstado_InvalidTransition_Returns400() throws Exception {
        UpdateEstadoRequest req = new UpdateEstadoRequest();
        req.setNewState(WeighingState.APROBADO);

        when(pesajeService.updateEstado(eq("abc123"), any()))
                .thenThrow(new IllegalWeighingStateException("Invalid state transition"));

        mockMvc.perform(put("/api/pesajes/abc123/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid state transition"));
    }

    @Test
    @DisplayName("PUT /api/pesajes/{id}/estado → 404 when record not found")
    void testUpdateEstado_NotFound_Returns404() throws Exception {
        UpdateEstadoRequest req = new UpdateEstadoRequest();
        req.setNewState(WeighingState.PESADO);

        when(pesajeService.updateEstado(eq("missing"), any()))
                .thenThrow(new ResourceNotFoundException("Pesaje not found: missing"));

        mockMvc.perform(put("/api/pesajes/missing/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pesaje not found: missing"));
    }

    @Test
    @DisplayName("PUT /api/pesajes/{id}/estado → 400 when newState is null")
    void testUpdateEstado_NullState_Returns400() throws Exception {
        String body = "{\"newState\": null}";

        mockMvc.perform(put("/api/pesajes/abc123/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ─── GET /api/pesajes ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/pesajes?fecha=2024-01-15 → 200 with list")
    void testGetPesajes_Returns200() throws Exception {
        PesajeResponse resp = PesajeResponse.builder()
                .id("abc")
                .scaleId("101")
                .currentState(WeighingState.INGRESADO)
                .weightCategory(WeightCategory.LIVIANO)
                .stateHistory(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(pesajeService.getPesajesByFecha(LocalDate.of(2024, 1, 15)))
                .thenReturn(List.of(resp));

        mockMvc.perform(get("/api/pesajes")
                        .param("fecha", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("abc"))
                .andExpect(jsonPath("$[0].currentState").value("INGRESADO"));
    }

    @Test
    @DisplayName("GET /api/pesajes?fecha=2024-06-01 → 200 empty list")
    void testGetPesajes_EmptyList_Returns200() throws Exception {
        when(pesajeService.getPesajesByFecha(any(LocalDate.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/pesajes")
                        .param("fecha", "2024-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/pesajes → 400 when fecha param is missing")
    void testGetPesajes_MissingFecha_Returns400() throws Exception {
        mockMvc.perform(get("/api/pesajes"))
                .andExpect(status().isBadRequest());
    }
}
