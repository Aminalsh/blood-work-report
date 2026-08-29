package io.everyonecodes.spring_module.controller;

import io.everyonecodes.spring_module.dto.CreateReportForm;
import io.everyonecodes.spring_module.dto.MarkerResultRequest;
import io.everyonecodes.spring_module.model.Marker;
import io.everyonecodes.spring_module.model.MarkerResultStatus;
import io.everyonecodes.spring_module.model.QualitativeValue;
import io.everyonecodes.spring_module.model.Report;
import io.everyonecodes.spring_module.model.TestType;
import io.everyonecodes.spring_module.service.ReportService;
import io.everyonecodes.spring_module.service.TestTypeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final TestTypeService testTypeService;
    private final ReportService reportService;

    public HomeController(
            TestTypeService testTypeService,
            ReportService reportService
    ) {
        this.testTypeService = testTypeService;
        this.reportService = reportService;
    }

    @GetMapping("/")
    public String showHomePage(
            @RequestParam(name = "testTypeId", required = false) Integer testTypeId,
            Model model
    ) {
        model.addAttribute("testTypes", testTypeService.findAll());

        if (testTypeId != null) {
            model.addAttribute(
                    "selectedTestType",
                    testTypeService.findById(testTypeId)
            );
        }

        return "index";
    }

    @PostMapping("/reports")
    public String createReport(
            @ModelAttribute CreateReportForm form,
            Model model
    ) {
        TestType testType = testTypeService.findById(form.getTestTypeId());
        Map<Integer, Marker> markersById = testType.getMarkers()
                .stream()
                .collect(Collectors.toMap(Marker::getId, Function.identity()));

        List<MarkerResultRequest> submittedResults = form.getResults()
                .stream()
                .map(input -> convertInput(input, markersById))
                .toList();

        Report report = reportService.createReport(
                form.getTestTypeId(),
                submittedResults
        );

        boolean allResultsNormal = !report.getResults().isEmpty()
                && report.getResults()
                .stream()
                .allMatch(result ->
                        result.getStatus() == MarkerResultStatus.NORMAL
                );

        model.addAttribute("allResultsNormal", allResultsNormal);
        model.addAttribute("testTypes", testTypeService.findAll());
        model.addAttribute("selectedTestType", testType);
        model.addAttribute("report", report);

        return "index";
    }

    private MarkerResultRequest convertInput(
            CreateReportForm.MarkerInput input,
            Map<Integer, Marker> markersById
    ) {
        Marker marker = markersById.get(input.getMarkerId());

        if (marker == null) {
            throw new IllegalArgumentException(
                    "Marker does not belong to the selected panel"
            );
        }

        if (input.getValue() == null || input.getValue().isBlank()) {
            throw new IllegalArgumentException(
                    "A value is required for " + marker.getName()
            );
        }

        String value = input.getValue().trim();

        return switch (marker.getResultType()) {
            case NUMERIC -> numericResult(marker, value);
            case QUALITATIVE -> new MarkerResultRequest(
                    marker.getId(),
                    null,
                    QualitativeValue.valueOf(value.toUpperCase(Locale.ROOT))
            );
            case MIXED -> mixedResult(marker, value);
        };
    }

    private MarkerResultRequest mixedResult(Marker marker, String value) {
        try {
            return new MarkerResultRequest(
                    marker.getId(),
                    null,
                    QualitativeValue.valueOf(value.toUpperCase(Locale.ROOT))
            );
        } catch (IllegalArgumentException exception) {
            return numericResult(marker, value);
        }
    }

    private MarkerResultRequest numericResult(
            Marker marker,
            String value
    ) {
        try {
            return new MarkerResultRequest(
                    marker.getId(),
                    new BigDecimal(value),
                    null
            );
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Enter a valid number for " + marker.getName(),
                    exception
            );
        }
    }
}
