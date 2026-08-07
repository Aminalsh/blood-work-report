package io.everyonecodes.spring_module.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "test_type_id", nullable = false)
    private TestType testType;

    @Column(name = "report_code", nullable = false, unique = true)
    private String code;

    @Column(name = "report_date", nullable = false)
    private LocalDate date;

    @OneToMany(
            mappedBy = "report",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ReportResult> results = new ArrayList<>();

    @Column(
            name = "interpretation",
            columnDefinition = "TEXT",
            nullable = false
    )
    private String interpretation;

    public void addResult(ReportResult result) {
        results.add(result);
        result.setReport(this);
    }


}