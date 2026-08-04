package io.everyonecodes.spring_module.service;

import io.everyonecodes.spring_module.model.Marker;
import io.everyonecodes.spring_module.repository.MarkerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarkerService {

    private final MarkerRepository markerRepository;

    public MarkerService(MarkerRepository markerRepository) {
        this.markerRepository = markerRepository;
    }
    public List<Marker> findAll() {
        return markerRepository.findAll();
    }

    public Marker findById(Integer id) {
        return markerRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Marker not found with id: " + id
                        )
                );
    }


}