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

    @Column(name = "unit")
    private String unit;

    @Column(name = "normal_min")
    private BigDecimal normalMin;

    @Column(name = "normal_max")
    private BigDecimal normalMax;

    @Column(
            name = "description",
            columnDefinition = "TEXT",
            nullable = false
    )
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false, length = 20)
    private ResultType resultType;

    @Enumerated(EnumType.STRING)
    @Column(name = "normal_qualitative_result", length = 30)
    private QualitativeResult normalQualitativeResult;

    @JsonIgnore
    @ManyToMany(mappedBy = "markers")
    private Set<TestType> testTypes = new HashSet<>();
}