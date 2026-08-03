package io.everyonecodes.spring_module.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "markers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Marker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "marker_id")
    private Integer id;

    @Column(name = "marker_name", nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String unit;

    @Column(name = "normal_min", nullable = false)
    private BigDecimal normalMin;

    @Column(name = "normal_max", nullable = false)
    private BigDecimal normalMax;

    @Column(name = "description", nullable = false)
    private String description;

    @JsonIgnore
    @ManyToMany(mappedBy = "markers")
    private Set<TestType> testTypes = new HashSet<>();
}