package io.everyonecodes.spring_module.repository;

import io.everyonecodes.spring_module.model.Marker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarkerRepository
        extends JpaRepository<Marker, Integer> {
}