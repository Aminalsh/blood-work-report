package io.everyonecodes.spring_module.repository;

import io.everyonecodes.spring_module.model.ReportResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportResultRepository
        extends JpaRepository<ReportResult, Integer> {
}