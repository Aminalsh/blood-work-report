package io.everyonecodes.spring_module.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "test_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_type_id")
    private Integer id;

    @Column(name = "test_name", nullable = false, unique = true)
    private String name;

    private String description;

    private String category;

    @ManyToMany
    @JoinTable(
            name = "test_type_markers",
            joinColumns = @JoinColumn(name = "test_type_id"),
            inverseJoinColumns = @JoinColumn(name = "marker_id")
    )

    @OrderBy("name ASC")
    private Set<Marker> markers = new HashSet<>();

}
