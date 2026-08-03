package io.everyonecodes.spring_module.service;

import io.everyonecodes.spring_module.model.*;
import io.everyonecodes.spring_module.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final TestTypeService testTypeService;

    public ReportService(ReportRepository reportRepository, TestTypeService testTypeService) {
        this.reportRepository = reportRepository;
        this.testTypeService = testTypeService;
    }

    @Transactional
    public Report createReport(Integer testTypeId, Map<Integer, BigDecimal> markerValues) {

        // Find the selected test
        TestType testType = testTypeService.findById(testTypeId);

        //  Check that the submitted values are correct
        validateMarkerValues(testType, markerValues);

        // Create the report
        Report report = new Report();
        report.setTestType(testType);
        report.setDate(LocalDate.now());
        report.setCode(generateReportCode());


        // Create one ReportResult for every marker
        for (Marker marker : testType.getMarkers()) {

            BigDecimal value = markerValues.get(marker.getId());
            ReportResult result = new ReportResult();
            result.setMarker(marker);
            result.setValue(value);

            // Adds result to report AND sets result.report
            report.addResult(result);
        }

        // Save report + its ReportResults
        return reportRepository.save(report);
    }


    private void validateMarkerValues(TestType testType, Map<Integer, BigDecimal> markerValues) {

        if (markerValues == null) {
            throw new IllegalArgumentException(
                    "Marker values cannot be null"
            );
        }


        Set<Integer> requiredMarkerIds =
                testType.getMarkers()
                        .stream()
                        .map(Marker::getId)
                        .collect(Collectors.toSet());


        Set<Integer> submittedMarkerIds = markerValues.keySet();


        if (!requiredMarkerIds.equals(submittedMarkerIds)) {

            throw new IllegalArgumentException(
                    "Submitted markers do not match " +
                            "the markers required for test type "
                            + testType.getName()
            );
        }


        boolean containsNullValue =
                markerValues.values()
                        .stream()
                        .anyMatch(value -> value == null);


        if (containsNullValue) {
            throw new IllegalArgumentException(
                    "Marker values cannot be null"
            );
        }
    }
    private String generateReportCode() {
        return "REPORT-" + UUID.randomUUID();
    }
}