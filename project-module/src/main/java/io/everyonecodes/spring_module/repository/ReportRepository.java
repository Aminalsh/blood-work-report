package io.everyonecodes.spring_module.repository;

import io.everyonecodes.spring_module.model.Report;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportRepository
        extends JpaRepository<Report, Integer> {

    @EntityGraph(attributePaths = {
            "testType",
            "results",
            "results.marker"
    })
    List<Report> findAllByOrderByIdDesc();

    @Override
    @EntityGraph(attributePaths = {
            "testType",
            "testType.markers",
            "results",
            "results.marker"
    })
    Optional<Report> findById(Integer id);
}