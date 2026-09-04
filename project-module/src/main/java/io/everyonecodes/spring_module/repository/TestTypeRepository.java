package io.everyonecodes.spring_module.repository;

import io.everyonecodes.spring_module.model.TestType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestTypeRepository

        extends JpaRepository<TestType, Integer> {


}