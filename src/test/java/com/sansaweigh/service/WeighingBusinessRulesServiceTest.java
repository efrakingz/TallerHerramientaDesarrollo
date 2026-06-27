package com.sansaweigh.service;

import com.sansaweigh.domain.WeightCategory;
import com.sansaweigh.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class WeighingBusinessRulesServiceTest {

    private WeighingBusinessRulesService service;

    @BeforeEach
    void setUp() {
        service = new WeighingBusinessRulesService();
    }

    // ─── Conversion ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Convert 1.337 kg → 1.0 Sansa")
    void testConvertKgToSansas() {
        double result = service.convertKgToSansas(1.337);
        assertThat(result).isCloseTo(1.0, within(0.001));
    }

    @Test
    @DisplayName("Convert 0 kg → 0 Sansas")
    void testConvertZero() {
        assertThat(service.convertKgToSansas(0.0)).isEqualTo(0.0);
    }

    // ─── Classification ──────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} Sansas → {1}")
    @CsvSource({
            "0.001, LIVIANO",
            "5.0,   LIVIANO",
            "10.0,  LIVIANO",
            "10.001,MEDIANO",
            "30.0,  MEDIANO",
            "50.0,  MEDIANO",
            "50.001,PESADO",
            "100.0, PESADO"
    })
    @DisplayName("Classify packages by weight in Sansas")
    void testClassify(double sansas, WeightCategory expected) {
        assertThat(service.classify(sansas)).isEqualTo(expected);
    }

    // ─── Night restriction ───────────────────────────────────────────────────

    @Test
    @DisplayName("PESADO rejected between 20:00 and midnight")
    void testNightRestriction_AfterTwenty() {
        LocalDateTime night = LocalDateTime.of(2024, 1, 1, 21, 0);
        assertThatThrownBy(() -> service.validateNightRestriction(WeightCategory.PESADO, night))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("20:00 and 06:00");
    }

    @Test
    @DisplayName("PESADO rejected at exactly 20:01")
    void testNightRestriction_ExactlyAfterNightStart() {
        LocalDateTime time = LocalDateTime.of(2024, 1, 1, 20, 1);
        assertThatThrownBy(() -> service.validateNightRestriction(WeightCategory.PESADO, time))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("PESADO rejected before 06:00 (early morning)")
    void testNightRestriction_BeforeSix() {
        LocalDateTime earlyMorning = LocalDateTime.of(2024, 1, 1, 3, 0);
        assertThatThrownBy(() -> service.validateNightRestriction(WeightCategory.PESADO, earlyMorning))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("PESADO rejected at midnight (00:00)")
    void testNightRestriction_AtMidnight() {
        LocalDateTime midnight = LocalDateTime.of(2024, 1, 1, 0, 0);
        assertThatThrownBy(() -> service.validateNightRestriction(WeightCategory.PESADO, midnight))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("PESADO allowed at exactly 06:00")
    void testNightRestriction_ExactlySixAM() {
        LocalDateTime sixAM = LocalDateTime.of(2024, 1, 1, 6, 0);
        assertThatNoException().isThrownBy(() ->
                service.validateNightRestriction(WeightCategory.PESADO, sixAM));
    }

    @Test
    @DisplayName("PESADO allowed at midday")
    void testNightRestriction_DayTime() {
        LocalDateTime day = LocalDateTime.of(2024, 1, 1, 12, 0);
        assertThatNoException().isThrownBy(() ->
                service.validateNightRestriction(WeightCategory.PESADO, day));
    }

    @Test
    @DisplayName("LIVIANO not restricted at any hour")
    void testNightRestriction_Liviano() {
        LocalDateTime night = LocalDateTime.of(2024, 1, 1, 22, 0);
        assertThatNoException().isThrownBy(() ->
                service.validateNightRestriction(WeightCategory.LIVIANO, night));
    }

    @Test
    @DisplayName("MEDIANO not restricted at night")
    void testNightRestriction_Mediano() {
        LocalDateTime night = LocalDateTime.of(2024, 1, 1, 23, 0);
        assertThatNoException().isThrownBy(() ->
                service.validateNightRestriction(WeightCategory.MEDIANO, night));
    }

    // ─── Prime scale rule ────────────────────────────────────────────────────

    @Test
    @DisplayName("Prime scaleId + odd day + PESADO → BusinessException")
    void testPrimeScaleRule_Violated() {
        LocalDateTime oddDay = LocalDateTime.of(2024, 1, 1, 12, 0); // Jan 1 = odd
        assertThatThrownBy(() ->
                service.validatePrimeScaleRule("7", WeightCategory.PESADO, oddDay))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("prime");
    }

    @Test
    @DisplayName("Prime scaleId (2) + odd day + PESADO → BusinessException")
    void testPrimeScaleRule_SmallestPrime() {
        LocalDateTime oddDay = LocalDateTime.of(2024, 1, 3, 12, 0);
        assertThatThrownBy(() ->
                service.validatePrimeScaleRule("2", WeightCategory.PESADO, oddDay))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Prime scaleId + even day + PESADO → passes")
    void testPrimeScaleRule_PrimeEvenDay() {
        LocalDateTime evenDay = LocalDateTime.of(2024, 1, 2, 12, 0); // Jan 2 = even
        assertThatNoException().isThrownBy(() ->
                service.validatePrimeScaleRule("7", WeightCategory.PESADO, evenDay));
    }

    @Test
    @DisplayName("Non-prime scaleId + odd day + PESADO → passes")
    void testPrimeScaleRule_NonPrime() {
        LocalDateTime oddDay = LocalDateTime.of(2024, 1, 1, 12, 0);
        assertThatNoException().isThrownBy(() ->
                service.validatePrimeScaleRule("4", WeightCategory.PESADO, oddDay));
    }

    @Test
    @DisplayName("scaleId=1 is NOT prime → no exception")
    void testPrimeScaleRule_One_NotPrime() {
        LocalDateTime oddDay = LocalDateTime.of(2024, 1, 1, 12, 0);
        assertThatNoException().isThrownBy(() ->
                service.validatePrimeScaleRule("1", WeightCategory.PESADO, oddDay));
    }

    @Test
    @DisplayName("Non-numeric scaleId does not trigger prime rule")
    void testPrimeScaleRule_NonNumericId() {
        LocalDateTime oddDay = LocalDateTime.of(2024, 1, 1, 12, 0);
        assertThatNoException().isThrownBy(() ->
                service.validatePrimeScaleRule("ABC", WeightCategory.PESADO, oddDay));
    }

    @Test
    @DisplayName("Prime rule only applies to PESADO category")
    void testPrimeScaleRule_NonPesado_NotApplied() {
        LocalDateTime oddDay = LocalDateTime.of(2024, 1, 1, 12, 0);
        assertThatNoException().isThrownBy(() ->
                service.validatePrimeScaleRule("7", WeightCategory.LIVIANO, oddDay));
        assertThatNoException().isThrownBy(() ->
                service.validatePrimeScaleRule("7", WeightCategory.MEDIANO, oddDay));
    }

    // ─── isPrime ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isPrime returns correct values for edge cases and primes")
    void testIsPrime() {
        assertThat(service.isPrime(0)).isFalse();
        assertThat(service.isPrime(1)).isFalse();
        assertThat(service.isPrime(2)).isTrue();
        assertThat(service.isPrime(3)).isTrue();
        assertThat(service.isPrime(4)).isFalse();
        assertThat(service.isPrime(5)).isTrue();
        assertThat(service.isPrime(7)).isTrue();
        assertThat(service.isPrime(9)).isFalse();
        assertThat(service.isPrime(11)).isTrue();
        assertThat(service.isPrime(25)).isFalse();
        assertThat(service.isPrime(97)).isTrue();
    }

    // ─── isOddDay ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isOddDay returns correct values")
    void testIsOddDay() {
        assertThat(service.isOddDay(LocalDateTime.of(2024, 1, 1, 0, 0))).isTrue();
        assertThat(service.isOddDay(LocalDateTime.of(2024, 1, 2, 0, 0))).isFalse();
        assertThat(service.isOddDay(LocalDateTime.of(2024, 1, 3, 0, 0))).isTrue();
        assertThat(service.isOddDay(LocalDateTime.of(2024, 1, 30, 0, 0))).isFalse();
        assertThat(service.isOddDay(LocalDateTime.of(2024, 1, 31, 0, 0))).isTrue();
    }
}
