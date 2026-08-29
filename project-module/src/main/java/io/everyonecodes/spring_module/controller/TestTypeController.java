package io.everyonecodes.spring_module.controller;

import io.everyonecodes.spring_module.model.Marker;
import io.everyonecodes.spring_module.model.TestType;
import io.everyonecodes.spring_module.service.TestTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/test-types")
public class TestTypeController {

    private final TestTypeService testTypeService;

    public TestTypeController(TestTypeService testTypeService) {
        this.testTypeService = testTypeService;
    }

    @GetMapping
    public List<TestType> findAll() {
        return testTypeService.findAll();
    }

    @GetMapping("/{testTypeId}/markers")
    public Set<Marker> findMarkers(@PathVariable Integer testTypeId) {
        return testTypeService.findMarkers(testTypeId);
    }
}
