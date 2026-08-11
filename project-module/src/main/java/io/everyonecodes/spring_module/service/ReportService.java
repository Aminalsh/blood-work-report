package io.everyonecodes.spring_module.service;

import io.everyonecodes.spring_module.dto.MarkerResultRequest;
import io.everyonecodes.spring_module.exception.ReportNotFoundException;
import io.everyonecodes.spring_module.interpretations.KidneyTestInterpretation;
import io.everyonecodes.spring_module.interpretations.LiverTestInterpretation;
import io.everyonecodes.spring_module.model.Marker;
import io.everyonecodes.spring_module.model.Report;
import io.everyonecodes.spring_module.model.ReportResult;
import io.everyonecodes.spring_module.model.ResultStatus;
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
    private final ResultEvaluator resultEvaluator;
    private final TestTypeService testTypeService;
    private final KidneyTestInterpretation kidneyTestInterpretation;
    private final LiverTestInterpretation liverTestInterpretation;

    public ReportService(
            ReportRepository reportRepository,
            TestTypeService testTypeService,
            ResultEvaluator resultEvaluator,
            KidneyTestInterpretation kidneyTestInterpretation,
            LiverTestInterpretation liverTestInterpretation
    ) {
        this.reportRepository = reportRepository;
        this.testTypeService = testTypeService;
        this.resultEvaluator = resultEvaluator;
        this.kidneyTestInterpretation = kidneyTestInterpretation;
        this.liverTestInterpretation = liverTestInterpretation;
    }

    @Transactional
    public Report createReport(Integer testTypeId, List<MarkerResultRequest> submittedResults) {
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
    public List<Report> findAll() {
        return reportRepository.findAllByOrderByDateDescIdDesc();
    }

    @Transactional(readOnly = true)
    public Report findById(Integer id) {
        return requireReport(id);
    }

    @Transactional
    public Report updateReport(
            Integer id,
            List<MarkerResultRequest> submittedResults
    ) {
        Report report = requireReport(id);
        TestType testType = report.getTestType();

        Map<Integer, MarkerResultRequest> resultsByMarkerId =
                validateAndIndexResults(testType, submittedResults);

        applyResults(report, testType, resultsByMarkerId);
        report.setInterpretation(createInterpretation(report));

        return reportRepository.save(report);
    }

    @Transactional
    public void deleteReport(Integer id) {
        Report report = requireReport(id);
        reportRepository.delete(report);
    }

    private Report requireReport(Integer id) {
        return reportRepository.findDetailedById(id)
                .orElseThrow(() -> new ReportNotFoundException(id));
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

        Map<Integer, ReportResult> existingResultsByMarkerId =
                report.getResults()
                        .stream()
                        .collect(Collectors.toMap(
                                result -> result.getMarker().getId(),
                                result -> result
                        ));

        for (Marker marker : testType.getMarkers()) {
            MarkerResultRequest request = resultsByMarkerId.get(marker.getId());

            validateResultInput(marker, request);

            ResultStatus status = resultEvaluator.evaluate(
                    marker,
                    request.numericValue(),
                    request.qualitativeValue()
            );

            ReportResult result = existingResultsByMarkerId.get(marker.getId());

            if (result == null) {
                result = new ReportResult();
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
        String testTypeName = report.getTestType().getName();

        return switch (testTypeName) {
            case KIDNEY_FUNCTION_TEST ->
                    kidneyTestInterpretation.interpret(report.getResults());
            case LIVER_FUNCTION_TEST ->
                    liverTestInterpretation.interpret(report.getResults());
            default -> throw new IllegalStateException(
                    "No interpretation is configured for test type: "
                            + testTypeName
            );
        };
    }

    private Map<Integer, MarkerResultRequest> validateAndIndexResults(
            TestType testType,
            List<MarkerResultRequest> submittedResults
    ) {

        if (submittedResults == null) {
            throw new IllegalArgumentException(
                    "Submitted results cannot be null"
            );
        }

        Map<Integer, MarkerResultRequest> resultsByMarkerId = new HashMap<>();

        for (MarkerResultRequest request : submittedResults) {

            if (request == null) {
                throw new IllegalArgumentException(
                        "A submitted result cannot be null"
                );
            }

            if (request.markerId() == null) {
                throw new IllegalArgumentException(
                        "Marker ID cannot be null"
                );
            }

            MarkerResultRequest previousRequest =
                    resultsByMarkerId.putIfAbsent(request.markerId(), request);

            if (previousRequest != null) {
                throw new IllegalArgumentException(
                        "Marker was submitted more than once: "
                                + request.markerId()
                );
            }
        }

        Set<Integer> requiredMarkerIds =
                testType.getMarkers()
                        .stream()
                        .map(Marker::getId)
                        .collect(Collectors.toSet());

        Set<Integer> submittedMarkerIds = resultsByMarkerId.keySet();

        if (!requiredMarkerIds.equals(submittedMarkerIds)) {
            throw new IllegalArgumentException(
                    "Submitted markers do not match the markers " +
                            "required for test type "
                            + testType.getName()
            );
        }

        return resultsByMarkerId;
    }

    private void validateResultInput(
            Marker marker,
            MarkerResultRequest request
    ) {

        boolean hasNumericValue = request.numericValue() != null;

        boolean hasQualitativeValue = request.qualitativeValue() != null;

        if (hasNumericValue == hasQualitativeValue) {
            throw new IllegalArgumentException(
                    "Exactly one numeric or qualitative value " +
                            "must be provided for marker "
                            + marker.getName()
            );
        }

        if (marker.getResultType() == null) {
            throw new IllegalStateException(
                    "Result type is not configured for marker: "
                            + marker.getName()
            );
        }

        switch (marker.getResultType()) {

            case NUMERIC -> {
                if (!hasNumericValue) {
                    throw new IllegalArgumentException(
                            marker.getName()
                                    + " requires a numeric value"
                    );
                }
            }

            case QUALITATIVE -> {
                if (!hasQualitativeValue) {
                    throw new IllegalArgumentException(
                            marker.getName()
                                    + " requires a qualitative value"
                    );
                }
            }

            case MIXED -> {
                // Exactly one value is already required above.
            }
        }
    }

    private String generateReportCode() {
        return "REPORT-" + UUID.randomUUID();
    }
}
