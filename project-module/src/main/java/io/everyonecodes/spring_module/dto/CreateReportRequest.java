package io.everyonecodes.spring_module.dto;

import java.util.List;

public record CreateReportRequest(
        Integer testTypeId,
        List<MarkerResultRequest> results
) {
}
