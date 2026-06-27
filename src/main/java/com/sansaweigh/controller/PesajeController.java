package com.sansaweigh.controller;

import com.sansaweigh.dto.*;
import com.sansaweigh.service.PesajeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pesajes")
@RequiredArgsConstructor
@Tag(name = "Pesajes", description = "API for managing package weighing records")
public class PesajeController {

    private final PesajeService pesajeService;

    @PostMapping
    @Operation(
            summary = "Create a new weighing record",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Record created successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "422", description = "Business rule violation")
            }
    )
    public ResponseEntity<PesajeResponse> createPesaje(
            @Valid @RequestBody CreatePesajeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pesajeService.createPesaje(request));
    }

    @PutMapping("/{id}/estado")
    @Operation(
            summary = "Update the state of a weighing record",
            responses = {
                    @ApiResponse(responseCode = "200", description = "State updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid state transition"),
                    @ApiResponse(responseCode = "404", description = "Record not found")
            }
    )
    public ResponseEntity<PesajeResponse> updateEstado(
            @PathVariable String id,
            @Valid @RequestBody UpdateEstadoRequest request) {
        return ResponseEntity.ok(pesajeService.updateEstado(id, request));
    }

    @GetMapping
    @Operation(
            summary = "Get weighing records filtered by date",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Records retrieved successfully")
            }
    )
    public ResponseEntity<List<PesajeResponse>> getPesajes(
            @Parameter(description = "Date filter in ISO format (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(pesajeService.getPesajesByFecha(fecha));
    }
}
