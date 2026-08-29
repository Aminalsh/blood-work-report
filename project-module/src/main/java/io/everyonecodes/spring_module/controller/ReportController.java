package io.everyonecodes.spring_module.controller;

import io.everyonecodes.spring_module.dto.CreateReportRequest;
import io.everyonecodes.spring_module.dto.MarkerResultResponse;
import io.everyonecodes.spring_module.dto.ReportResponse;
import io.everyonecodes.spring_module.dto.UpdateReportRequest;
import io.everyonecodes.spring_module.model.Report;
import io.everyonecodes.spring_module.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ReportResponse createReport(
            @RequestBody CreateReportRequest reportRequest
    ) {
        Report report = reportService.createReport(
                reportRequest.testTypeId(),
                reportRequest.results()
        );

        return toResponse(report);
    }

    @GetMapping
    public List<ReportResponse> findAllReports() {
        return reportService.findAllReports()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{reportId}")
    public ReportResponse findReport(@PathVariable Integer reportId) {
        return toResponse(reportService.findReportById(reportId));
    }

    @PutMapping("/{reportId}")
    public ReportResponse updateReport(
            @PathVariable Integer reportId,
            @RequestBody UpdateReportRequest reportRequest
    ) {
        Report report = reportService.updateReport(
                reportId,
                reportRequest.results()
        );

        return toResponse(report);
    }

    @DeleteMapping("/{reportId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReport(@PathVariable Integer reportId) {
        reportService.deleteReport(reportId);
    }

    private ReportResponse toResponse(Report report) {
        List<MarkerResultResponse> resultResponses = report.getResults()
                .stream()
                .map(result -> new MarkerResultResponse(
                        result.getMarker().getId(),
                        result.getMarker().getName(),
                        result.getNumericValue(),
                        result.getQualitativeValue(),
                        result.getMarker().getUnit(),
                        result.getStatus()
                ))
                .toList();

        return new ReportResponse(
                report.getId(),
                report.getCode(),
                report.getDate(),
                report.getTestType().getName(),
                resultResponses,
                report.getInterpretation()
        );
    }
}
