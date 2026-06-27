package com.sansaweigh.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "registro_pesaje")
public class RegistroPesaje {

    @Id
    private String id;

    private String scaleId;
    private String packageId;
    private double weightInSansas;
    private double weightInKg;
    private WeightCategory weightCategory;
    private WeighingState currentState;

    @Builder.Default
    private List<StateTransition> stateHistory = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
