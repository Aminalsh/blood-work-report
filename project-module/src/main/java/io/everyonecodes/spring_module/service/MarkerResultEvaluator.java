package io.everyonecodes.spring_module.service;

import io.everyonecodes.spring_module.model.Marker;
import io.everyonecodes.spring_module.model.QualitativeValue;
import io.everyonecodes.spring_module.model.MarkerResultStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class MarkerResultEvaluator {

    private static final Set<String> MAX_ONLY_MARKERS = Set.of(
            "LDL Cholesterol",
            "Total Cholesterol",
            "Non-HDL Cholesterol",
            "Triglycerides",
            "C-Reactive Protein",
            "Creatinine"
    );

    private static final Set<String> MIN_ONLY_MARKERS = Set.of(
            "HDL Cholesterol",
            "eGFR"
    );


    public MarkerResultStatus evaluate(
            Marker marker,
            BigDecimal numericValue,
            QualitativeValue qualitativeValue
    ) {

        if (marker == null) {
            throw new IllegalArgumentException(
                    "Marker cannot be null"
            );
        }

        if (marker.getResultType() == null) {
            throw new IllegalStateException(
                    "Result type is not configured for marker: "
                            + marker.getName()
            );
        }

        return switch (marker.getResultType()) {

            case NUMERIC ->
                    evaluateNumeric(marker, numericValue);

            case QUALITATIVE ->
                    evaluateQualitative(
                            marker,
                            qualitativeValue
                    );

            case MIXED ->
                    evaluateMixed(
                            marker,
                            numericValue,
                            qualitativeValue
                    );
        };
    }


    private MarkerResultStatus evaluateNumeric(
            Marker marker,
            BigDecimal value
    ) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Numeric value cannot be null for marker: "
                            + marker.getName()
            );
        }

        String markerName = marker.getName();

        if ("HbA1c".equals(markerName)) {
            return evaluateHbA1c(value);
        }

        if (MIN_ONLY_MARKERS.contains(markerName)) {
            return evaluateMinimum(marker, value);
        }

        if (MAX_ONLY_MARKERS.contains(markerName)) {
            return evaluateMaximum(marker, value);
        }

        return evaluateStandardRange(marker, value);
    }


    private MarkerResultStatus evaluateStandardRange(
            Marker marker,
            BigDecimal value
    ) {

        if (marker.getNormalMin() == null
                || marker.getNormalMax() == null) {

            throw new IllegalStateException(
                    "Marker requires both normalMin and normalMax: "
                            + marker.getName()
            );
        }

        if (value.compareTo(marker.getNormalMin()) < 0) {
            return MarkerResultStatus.LOW;
        }

        if (value.compareTo(marker.getNormalMax()) > 0) {
            return MarkerResultStatus.HIGH;
        }

        return MarkerResultStatus.NORMAL;
    }


    private MarkerResultStatus evaluateMinimum(
            Marker marker,
            BigDecimal value
    ) {

        if (marker.getNormalMin() == null) {
            throw new IllegalStateException(
                    "No normal minimum configured for marker: "
                            + marker.getName()
            );
        }

        if (value.compareTo(marker.getNormalMin()) < 0) {
            return MarkerResultStatus.LOW;
        }

        return MarkerResultStatus.NORMAL;
    }


    private MarkerResultStatus evaluateMaximum(
            Marker marker,
            BigDecimal value
    ) {

        if (marker.getNormalMax() == null) {
            throw new IllegalStateException(
                    "No normal maximum configured for marker: "
                            + marker.getName()
            );
        }

        if (value.compareTo(marker.getNormalMax()) > 0) {
            return MarkerResultStatus.HIGH;
        }

        return MarkerResultStatus.NORMAL;
    }


    private MarkerResultStatus evaluateHbA1c(
            BigDecimal value
    ) {

        BigDecimal normalLimit =
                new BigDecimal("5.7");

        if (value.compareTo(normalLimit) < 0) {
            return MarkerResultStatus.NORMAL;
        }

        return MarkerResultStatus.HIGH;
    }


    private MarkerResultStatus evaluateQualitative(
            Marker marker,
            QualitativeValue value
    ) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Qualitative value cannot be null for marker: "
                            + marker.getName()
            );
        }

        if (marker.getNormalQualitativeResult() == null) {
            throw new IllegalStateException(
                    "No normal qualitative result configured for marker: "
                            + marker.getName()
            );
        }

        if (value == marker.getNormalQualitativeResult()) {
            return MarkerResultStatus.NORMAL;
        }

        return MarkerResultStatus.ABNORMAL;
    }


    private MarkerResultStatus evaluateMixed(
            Marker marker,
            BigDecimal numericValue,
            QualitativeValue qualitativeValue
    ) {

        boolean hasNumericValue =
                numericValue != null;

        boolean hasQualitativeValue =
                qualitativeValue != null;

        if (hasNumericValue == hasQualitativeValue) {
            throw new IllegalArgumentException(
                    "Mixed marker requires exactly one numeric " +
                            "or qualitative value: "
                            + marker.getName()
            );
        }

        if (hasNumericValue) {
            return evaluateNumeric(
                    marker,
                    numericValue
            );
        }

        return evaluateQualitative(
                marker,
                qualitativeValue
        );
    }
}
