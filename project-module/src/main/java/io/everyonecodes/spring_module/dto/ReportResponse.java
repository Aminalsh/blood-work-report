package io.everyonecodes.spring_module.dto;

import java.time.LocalDate;
import java.util.List;

public record ReportResponse(
        Integer id,
        String code,
        LocalDate date,
        String testTypeName,
        List<MarkerResultResponse> results,
        String interpretation
) {
}
