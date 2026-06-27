package com.sansaweigh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sansaweigh.domain.ScaleSpecification;
import com.sansaweigh.domain.WeightCategory;
import com.sansaweigh.domain.WeighingState;
import com.sansaweigh.dto.CreatePesajeRequest;
import com.sansaweigh.dto.UpdateEstadoRequest;
import com.sansaweigh.integration.ExternalScaleClient;
import com.sansaweigh.repository.RegistroPesajeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack integration test using embedded MongoDB (Flapdoodle) and a mocked
 * ExternalScaleClient to avoid requiring a real Redis/external-API connection.
 * Redis operations in ExternalScaleClient are bypassed via @MockBean.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PesajeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegistroPesajeRepository repository;

    // Mock the external dependency so the full application context starts without Redis
    @MockBean
    private ExternalScaleClient externalScaleClient;

    private ScaleSpecification mockSpec;

    @BeforeEach
    void setUp() {
        mockSpec = ScaleSpecification.builder()
                .id("4")
                .name("Test Scale")
                .brand("SansaScale-Pro")
                .maxCapacity(200.0)
                .precision(0.01)
                .lastCalibrationOffset(0.0)
                .build();

        when(externalScaleClient.getScaleSpecifications(anyString())).thenReturn(mockSpec);
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    // ─── POST ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Integration: POST creates pesaje with INGRESADO state in MongoDB")
    void testCreateAndPersist() throws Exception {
        CreatePesajeRequest req = new CreatePesajeRequest();
        req.setScaleId("4");         // non-prime → no prime rule issue
        req.setPackageId("PKG-INT-001");
        req.setWeightInKg(5.0);      // 5/1.337 ≈ 3.74 Sansas → LIVIANO

        MvcResult result = mockMvc.perform(post("/api/pesajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.currentState").value("INGRESADO"))
                .andExpect(jsonPath("$.weightCategory").value("LIVIANO"))
                .andExpect(jsonPath("$.scaleId").value("4"))
                .andExpect(jsonPath("$.packageId").value("PKG-INT-001"))
                .andReturn();

        // Verify it's actually in MongoDB
        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().get(0).getCurrentState()).isEqualTo(WeighingState.INGRESADO);
    }

    @Test
    @DisplayName("Integration: POST → PUT → GET full state machine flow")
    void testFullStateMachineFlow() throws Exception {
        // 1. Create
        CreatePesajeRequest createReq = new CreatePesajeRequest();
        createReq.setScaleId("4");
        createReq.setPackageId("PKG-FLOW");
        createReq.setWeightInKg(5.0);

        String createJson = mockMvc.perform(post("/api/pesajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(createJson).get("id").asText();

        // 2. Transition to PESADO
        UpdateEstadoRequest toPesado = new UpdateEstadoRequest();
        toPesado.setNewState(WeighingState.PESADO);
        mockMvc.perform(put("/api/pesajes/" + id + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toPesado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentState").value("PESADO"));

        // 3. Transition to APROBADO
        UpdateEstadoRequest toAprobado = new UpdateEstadoRequest();
        toAprobado.setNewState(WeighingState.APROBADO);
        mockMvc.perform(put("/api/pesajes/" + id + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toAprobado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentState").value("APROBADO"));

        // 4. Transition to DESPACHADO
        UpdateEstadoRequest toDespachado = new UpdateEstadoRequest();
        toDespachado.setNewState(WeighingState.DESPACHADO);
        mockMvc.perform(put("/api/pesajes/" + id + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toDespachado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentState").value("DESPACHADO"));

        // 5. Verify state history has all transitions
        String finalJson = mockMvc.perform(get("/api/pesajes")
                        .param("fecha", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(finalJson).get(0).get("stateHistory").size())
                .isGreaterThanOrEqualTo(4); // initial + 3 transitions
    }

    @Test
    @DisplayName("Integration: invalid state transition returns 400")
    void testInvalidTransition_Returns400() throws Exception {
        CreatePesajeRequest createReq = new CreatePesajeRequest();
        createReq.setScaleId("4");
        createReq.setPackageId("PKG-BAD");
        createReq.setWeightInKg(5.0);

        String createJson = mockMvc.perform(post("/api/pesajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(createJson).get("id").asText();

        // Try to go directly from INGRESADO → APROBADO (illegal)
        UpdateEstadoRequest badReq = new UpdateEstadoRequest();
        badReq.setNewState(WeighingState.APROBADO);

        mockMvc.perform(put("/api/pesajes/" + id + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("Integration: update non-existent record returns 404")
    void testUpdateNonExistentRecord_Returns404() throws Exception {
        UpdateEstadoRequest req = new UpdateEstadoRequest();
        req.setNewState(WeighingState.PESADO);

        mockMvc.perform(put("/api/pesajes/nonexistent-id/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Integration: GET by date returns only today's records")
    void testGetByDate_ReturnsOnlyTodayRecords() throws Exception {
        // Create a record
        CreatePesajeRequest req = new CreatePesajeRequest();
        req.setScaleId("4");
        req.setPackageId("PKG-DATE");
        req.setWeightInKg(5.0);

        mockMvc.perform(post("/api/pesajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Query today
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        mockMvc.perform(get("/api/pesajes").param("fecha", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].packageId").value("PKG-DATE"));

        // Query yesterday (should be empty)
        String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        mockMvc.perform(get("/api/pesajes").param("fecha", yesterday))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Integration: MEDIANO package (between 10 and 50 Sansas) created correctly")
    void testCreatePesaje_MedianoCategory() throws Exception {
        CreatePesajeRequest req = new CreatePesajeRequest();
        req.setScaleId("4");
        req.setPackageId("PKG-MEDIANO");
        req.setWeightInKg(20.0); // 20/1.337 ≈ 14.96 Sansas → MEDIANO

        mockMvc.perform(post("/api/pesajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weightCategory").value("MEDIANO"));
    }
}
