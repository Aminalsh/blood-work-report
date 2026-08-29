package io.everyonecodes.spring_module.repository;

import io.everyonecodes.spring_module.model.MarkerResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarkerResultRepository
        extends JpaRepository<MarkerResult, Integer> {
}
