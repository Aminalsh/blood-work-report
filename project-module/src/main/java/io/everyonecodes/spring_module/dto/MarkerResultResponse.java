package io.everyonecodes.spring_module.dto;

import io.everyonecodes.spring_module.model.MarkerResultStatus;
import io.everyonecodes.spring_module.model.QualitativeValue;

import java.math.BigDecimal;

public record MarkerResultResponse(
        Integer markerId,
        String markerName,
        BigDecimal numericValue,
        QualitativeValue qualitativeValue,
        String unit,
        MarkerResultStatus status
) {
}
