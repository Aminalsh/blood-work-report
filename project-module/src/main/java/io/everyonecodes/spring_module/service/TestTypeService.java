package io.everyonecodes.spring_module.service;

import io.everyonecodes.spring_module.global_exception_handling.ResourceNotFoundException;
import io.everyonecodes.spring_module.model.Marker;
import io.everyonecodes.spring_module.model.TestType;
import io.everyonecodes.spring_module.repository.TestTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TestTypeService {

    private final TestTypeRepository testTypeRepository;

    public TestTypeService(TestTypeRepository testTypeRepository) {
        this.testTypeRepository = testTypeRepository;
    }


    public List<TestType> findAll() {
        return testTypeRepository.findAll();
    }


    public TestType findById(Integer id) {
        return testTypeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Test type not found with id: " + id
                        )
                );
    }

    @Transactional(readOnly = true)
    public Set<Marker> findMarkers(Integer id) {

        TestType testType = findById(id);

        return new HashSet<>(testType.getMarkers());
    }
}
