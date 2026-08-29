package io.everyonecodes.spring_module.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UpdateReportForm {

    private List<MarkerInput> results = new ArrayList<>();

    @Getter
    @Setter
    public static class MarkerInput {

        private Integer markerId;
        private String value;
    }
}
