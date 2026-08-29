package io.everyonecodes.spring_module.dto;

import java.util.List;

public record UpdateReportRequest(
        List<MarkerResultRequest> results
) {
}
