package io.everyonecodes.spring_module.repository;

import io.everyonecodes.spring_module.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository
        extends JpaRepository<Report, Integer> {
}