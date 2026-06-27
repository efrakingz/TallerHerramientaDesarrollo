package com.sansaweigh.service;

import com.sansaweigh.domain.WeightCategory;
import com.sansaweigh.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class WeighingBusinessRulesService {

    private static final double SANSA_TO_KG = 1.337;
    private static final double LIVIANO_MAX_SANSAS = 10.0;
    private static final double MEDIANO_MAX_SANSAS = 50.0;
    private static final LocalTime NIGHT_START = LocalTime.of(20, 0);
    private static final LocalTime NIGHT_END = LocalTime.of(6, 0);

    /**
     * Converts kilograms to the proprietary Sansa unit.
     * 1 Sansa = 1.337 kg
     */
    public double convertKgToSansas(double kg) {
        return kg / SANSA_TO_KG;
    }

    /**
     * Classifies the package weight into LIVIANO, MEDIANO, or PESADO.
     * - LIVIANO: 0–10 Sansas (inclusive)
     * - MEDIANO: >10 and ≤50 Sansas
     * - PESADO: >50 Sansas
     */
    public WeightCategory classify(double weightInSansas) {
        if (weightInSansas <= LIVIANO_MAX_SANSAS) {
            return WeightCategory.LIVIANO;
        } else if (weightInSansas <= MEDIANO_MAX_SANSAS) {
            return WeightCategory.MEDIANO;
        } else {
            return WeightCategory.PESADO;
        }
    }

    /**
     * PESADO packages cannot be processed between 20:00 and 06:00 server time.
     */
    public void validateNightRestriction(WeightCategory category, LocalDateTime now) {
        if (category == WeightCategory.PESADO) {
            LocalTime time = now.toLocalTime();
            boolean isNight = time.isAfter(NIGHT_START) || time.isBefore(NIGHT_END);
            if (isNight) {
                throw new BusinessException(
                        "PESADO packages cannot be processed between 20:00 and 06:00");
            }
        }
    }

    /**
     * Prima Scale Rule: If scaleId is prime AND today is an ODD calendar day,
     * PESADO packages cannot be registered.
     */
    public void validatePrimeScaleRule(String scaleId, WeightCategory category, LocalDateTime now) {
        if (category == WeightCategory.PESADO) {
            try {
                int id = Integer.parseInt(scaleId);
                if (isPrime(id) && isOddDay(now)) {
                    throw new BusinessException(
                            "Scale with prime ID cannot register PESADO packages on odd calendar days");
                }
            } catch (NumberFormatException e) {
                // scaleId is not numeric — rule does not apply
            }
        }
    }

    boolean isPrime(int n) {
        if (n < 2) return false;
        if (n == 2) return true;
        if (n % 2 == 0) return false;
        for (int i = 3; i * i <= n; i += 2) {
            if (n % i == 0) return false;
        }
        return true;
    }

    boolean isOddDay(LocalDateTime dateTime) {
        return dateTime.getDayOfMonth() % 2 != 0;
    }
}
