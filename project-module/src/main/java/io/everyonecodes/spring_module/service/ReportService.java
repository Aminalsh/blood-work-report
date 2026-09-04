package io.everyonecodes.spring_module.service;

import io.everyonecodes.spring_module.dto.MarkerResultRequest;
import io.everyonecodes.spring_module.global_exception_handling.ResourceNotFoundException;
import io.everyonecodes.spring_module.interpretations.KidneyReportInterpreter;
import io.everyonecodes.spring_module.interpretations.LiverReportInterpreter;
import io.everyonecodes.spring_module.model.Marker;
import io.everyonecodes.spring_module.model.MarkerResult;
import io.everyonecodes.spring_module.model.MarkerResultStatus;
import io.everyonecodes.spring_module.model.Report;
import io.everyonecodes.spring_module.model.TestType;
import io.everyonecodes.spring_module.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final String KIDNEY_FUNCTION_TEST = "Kidney Function Test";
    private static final String LIVER_FUNCTION_TEST = "Liver Function Test";

    private final ReportRepository reportRepository;
    private final MarkerResultEvaluator markerResultEvaluator;
    private final TestTypeService testTypeService;
    private final KidneyReportInterpreter kidneyReportInterpreter;
    private final LiverReportInterpreter liverReportInterpreter;

    public ReportService(
            ReportRepository reportRepository,
            MarkerResultEvaluator markerResultEvaluator,
            TestTypeService testTypeService,
            KidneyReportInterpreter kidneyReportInterpreter,
            LiverReportInterpreter liverReportInterpreter
    ) {
        this.reportRepository = reportRepository;
        this.markerResultEvaluator = markerResultEvaluator;
        this.testTypeService = testTypeService;
        this.kidneyReportInterpreter = kidneyReportInterpreter;
        this.liverReportInterpreter = liverReportInterpreter;
    }

    @Transactional
    public Report createReport(
            Integer testTypeId,
            List<MarkerResultRequest> submittedResults
    ) {
        if (testTypeId == null) {
            throw new IllegalArgumentException("Test type ID cannot be null");
        }
        TestType testType = testTypeService.findById(testTypeId);
        Map<Integer, MarkerResultRequest> resultsByMarkerId =
                validateAndIndexResults(testType, submittedResults);

        Report report = new Report();
        report.setTestType(testType);
        report.setDate(LocalDate.now());
        report.setCode(generateReportCode());

        applyResults(report, testType, resultsByMarkerId);
        report.setInterpretation(createInterpretation(report));

        return reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<Report> findAllReports() {
        return reportRepository.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public Report findReportById(Integer reportId) {
        if (reportId == null) {
            throw new IllegalArgumentException("Report ID cannot be null");
        }

        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Report not found with ID: " + reportId
                ));
    }

    @Transactional
    public Report updateReport(
            Integer reportId,
            List<MarkerResultRequest> submittedResults
    ) {
        Report report = findReportById(reportId);
        TestType testType = report.getTestType();
        Map<Integer, MarkerResultRequest> resultsByMarkerId =
                validateAndIndexResults(testType, submittedResults);

        applyResults(report, testType, resultsByMarkerId);
        report.setInterpretation(createInterpretation(report));

        return reportRepository.save(report);
    }

    @Transactional
    public void deleteReport(Integer reportId) {
        reportRepository.delete(findReportById(reportId));
    }

    private void applyResults(
            Report report,
            TestType testType,
            Map<Integer, MarkerResultRequest> resultsByMarkerId
    ) {
        Set<Integer> requiredMarkerIds = testType.getMarkers()
                .stream()
                .map(Marker::getId)
                .collect(Collectors.toSet());

        report.getResults().removeIf(result ->
                result.getMarker() == null
                        || !requiredMarkerIds.contains(result.getMarker().getId())
        );

        Map<Integer, MarkerResult> existingResultsByMarkerId = report.getResults()
                .stream()
                .collect(Collectors.toMap(
                        result -> result.getMarker().getId(),
                        result -> result
                ));

        for (Marker marker : testType.getMarkers()) {
            MarkerResultRequest request = resultsByMarkerId.get(marker.getId());
            validateResultInput(marker, request);

            MarkerResultStatus status = markerResultEvaluator.evaluate(
                    marker,
                    request.numericValue(),
                    request.qualitativeValue()
            );

            MarkerResult result = existingResultsByMarkerId.get(marker.getId());

            if (result == null) {
                result = new MarkerResult();
                report.addResult(result);
            } else {
                result.setReport(report);
            }

            result.setMarker(marker);
            result.setNumericValue(request.numericValue());
            result.setQualitativeValue(request.qualitativeValue());
            result.setStatus(status);
        }
    }

    private String createInterpretation(Report report) {
        return switch (report.getTestType().getName()) {
            case KIDNEY_FUNCTION_TEST ->
                    kidneyReportInterpreter.interpret(report.getResults());
            case LIVER_FUNCTION_TEST ->
                    liverReportInterpreter.interpret(report.getResults());
            default -> throw new IllegalStateException(
                    "No interpreter configured for test type: "
                            + report.getTestType().getName()
            );
        };
    }

    private Map<Integer, MarkerResultRequest> validateAndIndexResults(
            TestType testType,
            List<MarkerResultRequest> submittedResults
    ) {
        if (submittedResults == null) {
            throw new IllegalArgumentException("Submitted results cannot be null");
        }

        Map<Integer, MarkerResultRequest> resultsByMarkerId = new HashMap<>();

        for (MarkerResultRequest request : submittedResults) {
            if (request == null) {
                throw new IllegalArgumentException(
                        "A submitted result cannot be null"
                );
            }

            if (request.markerId() == null) {
                throw new IllegalArgumentException("Marker ID cannot be null");
            }

            MarkerResultRequest previousRequest =
                    resultsByMarkerId.putIfAbsent(request.markerId(), request);

            if (previousRequest != null) {
                throw new IllegalArgumentException(
                        "Marker was submitted more than once: " + request.markerId()
                );
            }
        }

        Set<Integer> requiredMarkerIds = testType.getMarkers()
                .stream()
                .map(Marker::getId)
                .collect(Collectors.toSet());

        if (!requiredMarkerIds.equals(resultsByMarkerId.keySet())) {
            throw new IllegalArgumentException(
                    "Submitted markers do not match the markers required for test type "
                            + testType.getName()
            );
        }

        return resultsByMarkerId;
    }

    private void validateResultInput(
            Marker marker,
            MarkerResultRequest request
    ) {
        if (marker.getResultType() == null) {
            throw new IllegalStateException(
                    "Result type is not configured for marker: " + marker.getName()
            );
        }

        boolean hasNumericValue = request.numericValue() != null;
        boolean hasQualitativeValue = request.qualitativeValue() != null;

        if (hasNumericValue == hasQualitativeValue) {
            throw new IllegalArgumentException(
                    "Exactly one numeric or qualitative value must be provided for marker "
                            + marker.getName()
            );
        }

        switch (marker.getResultType()) {
            case NUMERIC -> {
                if (!hasNumericValue) {
                    throw new IllegalArgumentException(
                            marker.getName() + " requires a numeric value"
                    );
                }
            }
            case QUALITATIVE -> {
                if (!hasQualitativeValue) {
                    throw new IllegalArgumentException(
                            marker.getName() + " requires a qualitative value"
                    );
                }
            }
            case MIXED -> {
                // The exactly-one-value validation above covers mixed markers.
            }
        }
    }

    private String generateReportCode() {
        return "REPORT-" + UUID.randomUUID();
    }
}
