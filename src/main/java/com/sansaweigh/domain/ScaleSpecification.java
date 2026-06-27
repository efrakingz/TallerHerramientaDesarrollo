package com.sansaweigh.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScaleSpecification implements Serializable {
    private String id;
    private String name;
    private String brand;
    private double maxCapacity;
    private double precision;
    private double lastCalibrationOffset;
}
