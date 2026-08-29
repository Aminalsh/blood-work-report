package io.everyonecodes.spring_module.dto;

import io.everyonecodes.spring_module.model.QualitativeValue;

import java.math.BigDecimal;

public record MarkerResultRequest(
        Integer markerId,
        BigDecimal numericValue,
        QualitativeValue qualitativeValue
) {
}
