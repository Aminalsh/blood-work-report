package io.everyonecodes.spring_module.interpretations;

import io.everyonecodes.spring_module.model.MarkerResult;
import io.everyonecodes.spring_module.model.MarkerResultStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KidneyReportInterpreter {

    /*
     * KIDNEY TEST EVALUATION ALGORITHM
     *
     * 1. kidney blood and urine markers:
     *    - Filtration: eGFR, creatinine and urea.
     *    - Electrolytes: sodium, potassium and chloride.
     *    - Minerals: calcium, phosphate and magnesium.
     *    - Other blood marker: uric acid.
     *    - Urine markers: protein, blood, leukocytes, nitrite, pH,
     *      specific gravity, glucose and ketones.
     *
     * 2. Classify kidney filtration using the eGFR value:
     *    - eGFR >= 90        -> G1: normal or high filtration.
     *    - eGFR 60–89       -> G2: mildly reduced filtration.
     *    - eGFR 45–59       -> G3a: mildly to moderately reduced.
     *    - eGFR 30–44       -> G3b: moderately to severely reduced.
     *    - eGFR 15–29       -> G4: severely reduced filtration.
     *    - eGFR < 15        -> G5: very severely reduced filtration.
     *
     * 3. Refine the filtration assessment using creatinine and urea:
     *    - Low eGFR with high creatinine supports reduced filtration.
     *    - Low eGFR with high creatinine and high urea provides a
     *      stronger combined reduced-filtration pattern.
     *    - Reduced filtration with abnormal urine protein provides
     *      additional evidence of a possible kidney-related pattern.
     *    - High creatinine with normal eGFR is reported as a mismatch
     *      that may require repeat testing or consideration of other influences.
     *    - High urea with normal filtration is interpreted cautiously because
     *      hydration, protein intake and other non-kidney factors can affect it.
     *    - Low urea is reported separately and does not indicate reduced filtration.
     *
     * 4. Evaluate urine protein and blood:
     *    - Protein abnormal and blood normal -> isolated protein finding.
     *    - Protein normal and blood abnormal -> isolated blood finding.
     *    - Protein and blood abnormal -> combined kidney or urinary-tract pattern.
     *    - Protein and blood normal -> no abnormality in these urine markers.
     *    - If protein or blood is abnormal together with reduced eGFR,
     *      the combined kidney-related pattern is more concerning.
     *
     * 5. Evaluate possible urinary-tract inflammation or infection:
     *    - Leukocytes abnormal and nitrite abnormal -> infection pattern supported.
     *    - Leukocytes abnormal and nitrite normal -> inflammation or infection
     *      remains possible but is not confirmed by nitrite.
     *    - Leukocytes normal and nitrite abnormal -> possible bacterial finding
     *      that should be interpreted with symptoms and urine culture.
     *    - Leukocytes and nitrite normal -> no infection pattern detected.
     *
     * 6. Evaluate urine concentration using specific gravity:
     *    - Low specific gravity -> unusually dilute urine.
     *    - High specific gravity -> concentrated urine.
     *    - High specific gravity with urine glucose or protein may partly reflect
     *      the substances present in the urine.
     *    - Normal specific gravity -> urine concentration is within range.
     *
     * 7. Evaluate urine pH:
     *    - Low pH -> acidic urine.
     *    - High pH -> alkaline urine.
     *    - High pH together with leukocytes and nitrite may support an
     *      infection-related urine pattern.
     *    - Urine pH alone is nonspecific and does not diagnose kidney disease.
     *
     * 8. Evaluate sodium and chloride together:
     *    - Both high -> combined high sodium/chloride pattern.
     *    - Both low -> combined low sodium/chloride pattern.
     *    - Only one abnormal -> report that marker separately.
     *    - Both normal -> no sodium/chloride abnormality detected.
     *
     * 9. Evaluate potassium:
     *    - High potassium with eGFR below 60 may be related to reduced
     *      kidney elimination and requires greater attention.
     *    - High potassium with preserved filtration may have another cause.
     *    - Low potassium is reported separately.
     *    - Normal potassium -> no potassium abnormality detected.
     *    - If sodium, potassium and chloride are all normal,
     *      report that electrolyte balance is within range.
     *
     * 10. Evaluate calcium, phosphate and magnesium:
     *     - eGFR below 30 with low calcium and high phosphate may indicate
     *       a kidney-related mineral-balance pattern.
     *     - Calcium or phosphate abnormalities without that complete pattern
     *       are interpreted separately.
     *     - High magnesium with eGFR below 60 may be related to reduced
     *       kidney elimination.
     *     - Other magnesium abnormalities are reported separately.
     *
     * 11. Evaluate uric acid:
     *     - High uric acid is reported with possible influences such as
     *       reduced elimination, dehydration, diet or increased cell breakdown.
     *     - Low uric acid is reported separately.
     *     - Uric acid alone does not determine kidney filtration.
     *
     * 12. Evaluate urine glucose and ketones:
     *     - Glucose and ketones abnormal together -> combined metabolic pattern.
     *     - Only glucose abnormal -> isolated urine-glucose finding.
     *     - Only ketones abnormal -> isolated urine-ketone finding.
     *     - Both normal -> no urine glucose/ketone abnormality detected.
     *
     * 13. Generate the overall assessment:
     *     - If every result is normal -> no abnormalities identified.
     *     - If only eGFR is mildly reduced -> report isolated mild reduction.
     *     - If eGFR is normal but other markers are abnormal -> filtration appears
     *       preserved, but the other abnormalities are still reported.
     *     - If eGFR is reduced -> state its G-category and combine it with
     *       supporting creatinine, urea and urine findings.
     *     - Independently report urinary, infection and glucose/ketone patterns.
     *
     */


    private static final BigDecimal eGFR_90 = new BigDecimal("90");
    private static final BigDecimal eGFR_60 = new BigDecimal("60");
    private static final BigDecimal eGFR_45 = new BigDecimal("45");
    private static final BigDecimal eGFR_30 = new BigDecimal("30");
    private static final BigDecimal eGFR_15 = new BigDecimal("15");

    private static final Set<String> requiredMarkers = Set.of(
            "Creatinine", "eGFR", "Urea", "Uric Acid", "Sodium", "Potassium", "Chloride", "Calcium", "Phosphate", "Magnesium", "Urine Protein", "Urine Blood (Erythrocytes)", "Urine Leukocytes", "Urine Nitrite", "Urine pH", "Urine Specific Gravity", "Urine Glucose", "Urine Ketones"
    );

    public String interpret(List<MarkerResult> results) {

        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException(
                    "Kidney test results cannot be empty"
            );
        }

        Map<String, MarkerResult> resultsByName = results.stream()
                        .collect(Collectors.toMap(
                                result -> result.getMarker().getName(),
                                result -> result));

        if (!resultsByName.keySet().equals(requiredMarkers)) {

            Set<String> missingMarkers = requiredMarkers.stream()
                            .filter(marker -> !resultsByName.containsKey(marker))
                            .collect(Collectors.toSet());

            throw new IllegalStateException(
                    "Incorrect Kidney Function Test configuration. " + "Missing markers: " + missingMarkers);
        }

        MarkerResult creatinine = resultsByName.get("Creatinine");
        MarkerResult eGFR = resultsByName.get("eGFR");
        MarkerResult urea = resultsByName.get("Urea");
        MarkerResult uricAcid = resultsByName.get("Uric Acid");
        MarkerResult sodium = resultsByName.get("Sodium");
        MarkerResult potassium = resultsByName.get("Potassium");
        MarkerResult chloride = resultsByName.get("Chloride");
        MarkerResult calcium = resultsByName.get("Calcium");
        MarkerResult phosphate = resultsByName.get("Phosphate");
        MarkerResult magnesium = resultsByName.get("Magnesium");
        MarkerResult urineProtein = resultsByName.get("Urine Protein");
        MarkerResult urineBlood = resultsByName.get("Urine Blood (Erythrocytes)");
        MarkerResult urineLeukocytes = resultsByName.get("Urine Leukocytes");
        MarkerResult urineNitrite = resultsByName.get("Urine Nitrite");
        MarkerResult urinePh = resultsByName.get("Urine pH");
        MarkerResult urineSpecificGravity = resultsByName.get("Urine Specific Gravity");
        MarkerResult urineGlucose = resultsByName.get("Urine Glucose");
        MarkerResult urineKetones = resultsByName.get("Urine Ketones");

        BigDecimal egfrValue = eGFR.getNumericValue();

        boolean egfrBelow90 = egfrValue.compareTo(eGFR_90) < 0;
        boolean egfrBelow60 = egfrValue.compareTo(eGFR_60) < 0;
        boolean egfrBelow30 = egfrValue.compareTo(eGFR_30) < 0;
        boolean egfrBelow15 = egfrValue.compareTo(eGFR_15) < 0;
        String egfrCategory;
        String egfrMeaning;

        if (egfrValue.compareTo(eGFR_90) >= 0) {
            egfrCategory = "G1";
            egfrMeaning = "kidney filtration is normal or high";

        } else if (egfrValue.compareTo(eGFR_60) >= 0) {

            egfrCategory = "G2";
            egfrMeaning = "kidney filtration is mildly lower than expected";

        } else if (egfrValue.compareTo(eGFR_45) >= 0) {

            egfrCategory = "G3a";
            egfrMeaning = "kidney filtration is mildly to moderately reduced";

        } else if (egfrValue.compareTo(eGFR_30) >= 0) {

            egfrCategory = "G3b";
            egfrMeaning = "kidney filtration is moderately to severely reduced";

        } else if (egfrValue.compareTo(eGFR_15) >= 0) {

            egfrCategory = "G4";
            egfrMeaning = "kidney filtration is severely reduced";

        } else {

            egfrCategory = "G5";
            egfrMeaning = "kidney filtration is very severely reduced";
        }


        boolean creatinineHigh = isHigh(creatinine);
        boolean ureaHigh = isHigh(urea);
        boolean proteinAbnormal = isAbnormal(urineProtein);
        boolean bloodAbnormal = isAbnormal(urineBlood);
        boolean leukocytesAbnormal = isAbnormal(urineLeukocytes);
        boolean nitriteAbnormal = isAbnormal(urineNitrite);
        boolean glucoseAbnormal = isAbnormal(urineGlucose);
        boolean ketonesAbnormal = isAbnormal(urineKetones);
        boolean kidneyOrUrinaryPattern = proteinAbnormal || bloodAbnormal;
        boolean infectionPattern = leukocytesAbnormal && nitriteAbnormal;
        boolean glucoseAndKetonesPattern = glucoseAbnormal && ketonesAbnormal;

        boolean allResultsNormal = results.stream()
                .allMatch(result -> result.getStatus() == MarkerResultStatus.NORMAL);

        boolean onlyEgfrOutsideRange = results.stream()
                        .filter(result -> !result.getMarker().getName().equals("eGFR"))
                        .allMatch(result -> result.getStatus() == MarkerResultStatus.NORMAL);



        List<String> overallAssessment = new ArrayList<>();
        List<String> filtrationFindings = new ArrayList<>();
        List<String> urineFindings = new ArrayList<>();
        List<String> electrolyteAndMineralFindings = new ArrayList<>();
        List<String> otherFindings = new ArrayList<>();
        List<String> possibleInfluences = new ArrayList<>();

        if (egfrBelow15) {

            overallAssessment.add(
                    "The eGFR is "
                            + displayValue(eGFR)
                            + ". This falls within the G5 range, meaning "
                            + "kidney filtration is very severely reduced."
            );

        } else if (egfrBelow30) {

            overallAssessment.add(
                    "The eGFR is "
                            + displayValue(eGFR)
                            + ". This falls within the G4 range, meaning "
                            + "kidney filtration is severely reduced."
            );

        } else if (egfrBelow60) {

            if (creatinineHigh && ureaHigh && proteinAbnormal) {

                overallAssessment.add(
                        "Kidney filtration is reduced. Creatinine and "
                                + "urea are elevated, and protein is present "
                                + "in the urine. Together, these findings "
                                + "form a stronger pattern of reduced kidney "
                                + "function than any one result alone."
                );

            } else if (creatinineHigh && ureaHigh) {

                overallAssessment.add(
                        "Kidney filtration is reduced, while creatinine "
                                + "and urea are elevated. This pattern may "
                                + "mean that the kidneys are removing waste "
                                + "products from the blood less effectively."
                );

            } else if (creatinineHigh) {

                overallAssessment.add(
                        "Kidney filtration is reduced and creatinine is "
                                + "elevated. These related findings support "
                                + "a pattern of reduced kidney filtration."
                );

            } else {

                overallAssessment.add(
                        "The eGFR is "
                                + displayValue(eGFR)
                                + ". This falls within the "
                                + egfrCategory
                                + " range, meaning "
                                + egfrMeaning
                                + "."
                );
            }

        } else if (egfrBelow90) {

            if (onlyEgfrOutsideRange) {

                overallAssessment.add(
                        "The eGFR is "
                                + displayValue(eGFR)
                                + ". This falls within the G2 range, meaning "
                                + "kidney filtration is mildly lower than "
                                + "expected. The remaining kidney-related "
                                + "markers are within their expected ranges."
                );

            } else {

                overallAssessment.add(
                        "The eGFR is "
                                + displayValue(eGFR)
                                + ". Kidney filtration is mildly lower than "
                                + "expected, and other findings are explained "
                                + "in the sections below."
                );
            }

        } else if (allResultsNormal) {

            overallAssessment.add(
                    "No abnormalities were found among the evaluated "
                            + "kidney filtration, electrolyte, mineral and "
                            + "urine markers."
            );

        } else {

            overallAssessment.add(
                    "Kidney filtration is within the expected range, but "
                            + "one or more other kidney-related or urine "
                            + "markers are outside their expected ranges."
            );
        }

        /*
         * Independent patterns are added separately because several
         * different patterns can be present at the same time.
         */
        if (kidneyOrUrinaryPattern) {

            if (egfrBelow60) {

                overallAssessment.add(
                        "Protein or blood in the urine appears together "
                                + "with reduced filtration, suggesting an "
                                + "additional possible problem involving "
                                + "the kidneys or urinary tract."
                );

            } else {

                overallAssessment.add(
                        "Protein or blood is present in the urine even "
                                + "though kidney filtration is within or "
                                + "close to the expected range."
                );
            }
        }

        if (infectionPattern) {

            overallAssessment.add(
                    "Leukocytes and nitrite are both present in the urine. "
                            + "This pattern may occur with a bacterial "
                            + "urinary-tract infection."
            );
        }

        if (glucoseAndKetonesPattern) {

            overallAssessment.add(
                    "Glucose and ketones are both present in the urine. "
                            + "This is mainly a metabolic finding rather "
                            + "than a direct sign of reduced kidney filtration."
            );
        }



         // KIDNEY FILTRATION


        filtrationFindings.add(
                "eGFR is "
                        + displayValue(eGFR)
                        + ". This falls within the "
                        + egfrCategory
                        + " range, meaning "
                        + egfrMeaning
                        + "."
        );

        if (creatinineHigh && egfrBelow90) {

            filtrationFindings.add(
                    "Creatinine is elevated at "
                            + displayValue(creatinine)
                            + ". Creatinine is used when calculating eGFR, "
                            + "so the elevated creatinine and lower eGFR are "
                            + "closely related findings."
            );

        } else if (creatinineHigh) {

            filtrationFindings.add(
                    "Creatinine is elevated at "
                            + displayValue(creatinine)
                            + ", while eGFR remains in the G1 range. The "
                            + "possible influences listed later in this "
                            + "report may help explain this difference."
            );

        } else {

            filtrationFindings.add(
                    "Creatinine is within its expected range at "
                            + displayValue(creatinine)
                            + "."
            );
        }

        if (ureaHigh && egfrBelow60) {

            filtrationFindings.add(
                    "Urea is elevated at "
                            + displayValue(urea)
                            + ". Together with the reduced eGFR, this may "
                            + "mean that waste products are being removed "
                            + "less effectively."
            );

        } else if (ureaHigh) {

            filtrationFindings.add(
                    "Urea is elevated at "
                            + displayValue(urea)
                            + ", but eGFR is not substantially reduced. "
                            + "The elevated urea therefore does not clearly "
                            + "form a reduced-filtration pattern."
            );

        } else if (isLow(urea)) {

            filtrationFindings.add(
                    "Urea is below its expected range at "
                            + displayValue(urea)
                            + ". Low urea is not a typical sign of reduced "
                            + "kidney filtration."
            );

        } else {

            filtrationFindings.add(
                    "Urea is within its expected range at "
                            + displayValue(urea)
                            + "."
            );
        }


          // URINE FINDINGS



        if (proteinAbnormal && bloodAbnormal) {

            urineFindings.add(
                    "Urine protein is "
                            + displayValue(urineProtein)
                            + " and urine blood is "
                            + displayValue(urineBlood)
                            + ". This combination may indicate a problem "
                            + "involving the kidneys or urinary tract. It "
                            + "does not show exactly where the finding "
                            + "comes from."
            );

        } else if (proteinAbnormal) {

            urineFindings.add(
                    "Urine protein is "
                            + displayValue(urineProtein)
                            + ". If protein continues to appear in the "
                            + "urine, it may be a sign of kidney damage."
            );

        } else if (bloodAbnormal) {

            urineFindings.add(
                    "Urine blood is "
                            + displayValue(urineBlood)
                            + ". The blood may come from the kidneys or "
                            + "from another part of the urinary tract."
            );

        } else {

            urineFindings.add(
                    "Protein and blood were not detected in the urine."
            );
        }

        if (leukocytesAbnormal && nitriteAbnormal) {

            urineFindings.add(
                    "Urine leukocytes are "
                            + displayValue(urineLeukocytes)
                            + " and nitrite is "
                            + displayValue(urineNitrite)
                            + ". This combination may occur with a "
                            + "bacterial urinary-tract infection."
            );

        } else if (leukocytesAbnormal) {

            urineFindings.add(
                    "Urine leukocytes are "
                            + displayValue(urineLeukocytes)
                            + ", while nitrite is negative. This may occur "
                            + "with inflammation, infection or contamination "
                            + "of the urine sample."
            );

        } else if (nitriteAbnormal) {

            urineFindings.add(
                    "Urine nitrite is "
                            + displayValue(urineNitrite)
                            + ", while leukocytes are negative. Nitrite may "
                            + "appear when certain bacteria are present."
            );

        } else {

            urineFindings.add(
                    "Leukocytes and nitrite were not detected in the urine."
            );
        }

        if (isLow(urineSpecificGravity)) {

            urineFindings.add(
                    "Urine specific gravity is low at "
                            + displayValue(urineSpecificGravity)
                            + ". This means the urine is dilute."
            );

        } else if (isHigh(urineSpecificGravity)) {

            if (proteinAbnormal || glucoseAbnormal) {

                urineFindings.add(
                        "Urine specific gravity is elevated at "
                                + displayValue(urineSpecificGravity)
                                + ". The urine is concentrated, and the "
                                + "detected protein or glucose may also "
                                + "contribute to the higher result."
                );

            } else {

                urineFindings.add(
                        "Urine specific gravity is elevated at "
                                + displayValue(urineSpecificGravity)
                                + ". This means the urine is concentrated."
                );
            }

        } else {

            urineFindings.add(
                    "Urine concentration is within the expected range, "
                            + "with a specific gravity of "
                            + displayValue(urineSpecificGravity)
                            + "."
            );
        }

        if (isHigh(urinePh)) {

            if (infectionPattern) {

                urineFindings.add(
                        "Urine pH is alkaline at "
                                + displayValue(urinePh)
                                + ". Together with leukocytes and nitrite, "
                                + "this may support the possible infection "
                                + "pattern."
                );

            } else {

                urineFindings.add(
                        "Urine pH is above its expected range at "
                                + displayValue(urinePh)
                                + ", meaning the urine is more alkaline."
                );
            }

        } else if (isLow(urinePh)) {

            urineFindings.add(
                    "Urine pH is below its expected range at "
                            + displayValue(urinePh)
                            + ", meaning the urine is more acidic."
            );

        } else {

            urineFindings.add(
                    "Urine pH is within its expected range at "
                            + displayValue(urinePh)
                            + "."
            );
        }



         // FLUID AND ELECTROLYTE BALANCE



        boolean sodiumHigh = isHigh(sodium);
        boolean sodiumLow = isLow(sodium);
        boolean chlorideHigh = isHigh(chloride);
        boolean chlorideLow = isLow(chloride);

        if (sodiumHigh && chlorideHigh) {

            electrolyteAndMineralFindings.add(
                    "Sodium and chloride are both elevated at "
                            + displayValue(sodium)
                            + " and "
                            + displayValue(chloride)
                            + ". This may mean that the balance between "
                            + "body water and these electrolytes is disturbed."
            );

        } else if (sodiumLow && chlorideLow) {

            electrolyteAndMineralFindings.add(
                    "Sodium and chloride are both reduced at "
                            + displayValue(sodium)
                            + " and "
                            + displayValue(chloride)
                            + ". This may mean that fluid and electrolyte "
                            + "balance is disturbed."
            );

        } else {

            if (sodiumHigh) {
                electrolyteAndMineralFindings.add(
                        "Sodium is elevated at "
                                + displayValue(sodium)
                                + ". This may mean that the balance between "
                                + "body water and sodium is disturbed."
                );
            }

            if (sodiumLow) {
                electrolyteAndMineralFindings.add(
                        "Sodium is reduced at "
                                + displayValue(sodium)
                                + ". This may mean that the balance between "
                                + "body water and sodium is disturbed."
                );
            }

            if (chlorideHigh) {
                electrolyteAndMineralFindings.add(
                        "Chloride is elevated at "
                                + displayValue(chloride)
                                + ". This may occur with changes in fluid "
                                + "or acid-base balance."
                );
            }

            if (chlorideLow) {
                electrolyteAndMineralFindings.add(
                        "Chloride is reduced at "
                                + displayValue(chloride)
                                + ". This may occur with changes in fluid "
                                + "or acid-base balance."
                );
            }
        }

        if (isHigh(potassium) && egfrBelow60) {

            electrolyteAndMineralFindings.add(
                    "Potassium is elevated at "
                            + displayValue(potassium)
                            + " while kidney filtration is reduced. The "
                            + "kidneys may be removing potassium less "
                            + "effectively."
            );

        } else if (isHigh(potassium)) {

            electrolyteAndMineralFindings.add(
                    "Potassium is elevated at "
                            + displayValue(potassium)
                            + ", although kidney filtration is not "
                            + "substantially reduced."
            );

        } else if (isLow(potassium)) {

            electrolyteAndMineralFindings.add(
                    "Potassium is reduced at "
                            + displayValue(potassium)
                            + ". This is not the usual pattern caused by "
                            + "reduced removal of potassium by the kidneys."
            );
        }

        if (
                isNormal(sodium)
                        && isNormal(potassium)
                        && isNormal(chloride)
        ) {

            electrolyteAndMineralFindings.add(
                    "Sodium, potassium and chloride are within their "
                            + "expected ranges."
            );
        }



          // MINERAL BALANCE


        boolean kidneyMineralPattern = egfrBelow30 && isLow(calcium) && isHigh(phosphate);

        if (kidneyMineralPattern) {

            electrolyteAndMineralFindings.add(
                    "Calcium is reduced at "
                            + displayValue(calcium)
                            + " and phosphate is elevated at "
                            + displayValue(phosphate)
                            + ". Together with the severely reduced eGFR, "
                            + "this combination may mean that the kidneys "
                            + "are having difficulty keeping calcium and phosphate balanced"
            );

        } else {

            if (isLow(calcium)) {
                electrolyteAndMineralFindings.add(
                        "Calcium is reduced at "
                                + displayValue(calcium)
                                + ". Calcium balance depends on more than "
                                + "kidney function alone."
                );
            }

            if (isHigh(calcium)) {
                electrolyteAndMineralFindings.add(
                        "Calcium is elevated at "
                                + displayValue(calcium)
                                + ". This is not a typical isolated sign of reduced kidney filtration."

                );
            }

            if (isHigh(phosphate)) {

                if (egfrBelow30) {

                    electrolyteAndMineralFindings.add(
                            "Phosphate is elevated at "
                                    + displayValue(phosphate)
                                    + " while kidney filtration is severely "
                                    + "reduced. The kidneys may be removing "
                                    + "phosphate less effectively."
                    );

                } else {

                    electrolyteAndMineralFindings.add(
                            "Phosphate is elevated at "
                                    + displayValue(phosphate)
                                    + ", although kidney filtration is not "
                                    + "severely reduced."
                    );
                }
            }

            if (isLow(phosphate)) {
                electrolyteAndMineralFindings.add(
                        "Phosphate is reduced at "
                                + displayValue(phosphate)
                                + ". Low phosphate is not a typical pattern "
                                + "of reduced kidney filtration."
                );
            }
        }

        if (isHigh(magnesium) && egfrBelow60) {

            electrolyteAndMineralFindings.add(
                    "Magnesium is elevated at "
                            + displayValue(magnesium)
                            + " while kidney filtration is reduced. The "
                            + "kidneys may be removing magnesium less effectively."
            );

        } else if (isHigh(magnesium)) {

            electrolyteAndMineralFindings.add(
                    "Magnesium is elevated at "
                            + displayValue(magnesium)
                            + ", although kidney filtration is not substantially reduced."
            );

        } else if (isLow(magnesium)) {

            electrolyteAndMineralFindings.add(
                    "Magnesium is reduced at "
                            + displayValue(magnesium)
                            + ". This is not the usual pattern caused by "
                            + "reduced magnesium removal by the kidneys."
            );
        }

        if (
                isNormal(calcium)
                        && isNormal(phosphate)
                        && isNormal(magnesium)
        ) {

            electrolyteAndMineralFindings.add(
                    "Calcium, phosphate and magnesium are within their expected ranges."
            );
        }

        /*
         * ============================================================
         * OTHER FINDINGS
         * ============================================================
         */

        if (isHigh(uricAcid) && egfrBelow60) {

            otherFindings.add(
                    "Uric acid is elevated at "
                            + displayValue(uricAcid)
                            + " while kidney filtration is reduced. Reduced "
                            + "removal of uric acid by the kidneys may "
                            + "contribute to this result."
            );

        } else if (isHigh(uricAcid)) {

            otherFindings.add(
                    "Uric acid is elevated at "
                            + displayValue(uricAcid)
                            + ", although kidney filtration is not substantially reduced."
            );

        } else if (isLow(uricAcid)) {

            otherFindings.add(
                    "Uric acid is below its expected range at "
                            + displayValue(uricAcid)
                            + ". This is not a typical sign of reduced kidney filtration."
            );

        } else {

            otherFindings.add(
                    "Uric acid is within its expected range at "
                            + displayValue(uricAcid) + "."
            );
        }

        if (glucoseAbnormal && ketonesAbnormal) {

            otherFindings.add(
                    "Urine glucose is "
                            + displayValue(urineGlucose)
                            + " and urine ketones are "
                            + displayValue(urineKetones)
                            + ". This combination suggests a metabolic "
                            + "change rather than a direct kidney-filtration problem."
            );

        } else if (glucoseAbnormal) {

            otherFindings.add(
                    "Urine glucose is "
                            + displayValue(urineGlucose)
                            + ". This may occur when blood glucose is high "
                            + "or when the kidneys release glucose into the urine more easily"
            );

        } else if (ketonesAbnormal) {

            otherFindings.add(
                    "Urine ketones are "
                            + displayValue(urineKetones)
                            + ". This is mainly a metabolic finding rather "
                            + "than a direct sign of reduced kidney filtration."
            );

        } else {

            otherFindings.add(
                    "Glucose and ketones were not detected in the urine."
            );
        }



         // POSSIBLE INFLUENCES FOR ABNORMAL MARKERS

        for (MarkerResult result : results) {

            if (isNormal(result)) {
                continue;
            }

            String influence = result.getMarker().getPossibleInfluences();

            if (!influence.isBlank()) {
                possibleInfluences.add(
                        result.getMarker().getName() + " (" + displayValue(result) + "): " + influence
                );
            }
        }

         // BUILD THE FINAL REPORT

        StringBuilder interpretation = new StringBuilder();

        interpretation.append("KIDNEY FUNCTION TEST – INTERPRETATION\n\n");

        appendSection(interpretation, "OVERALL ASSESSMENT", overallAssessment);

        appendSection(interpretation, "KIDNEY FILTRATION", filtrationFindings);

        appendSection(interpretation, "URINE FINDINGS", urineFindings);

        appendSection(interpretation, "ELECTROLYTES AND MINERALS", electrolyteAndMineralFindings);

        appendSection(interpretation, "OTHER FINDINGS", otherFindings);

        appendSection(interpretation, "POSSIBLE INFLUENCES ON ABNORMAL RESULTS", possibleInfluences);

        return interpretation.toString();
    }



    private boolean isNormal(MarkerResult result) {
        return result.getStatus() == MarkerResultStatus.NORMAL;
    }

    private boolean isHigh(MarkerResult result) {
        return result.getStatus() == MarkerResultStatus.HIGH;
    }

    private boolean isLow(MarkerResult result) {
        return result.getStatus() == MarkerResultStatus.LOW;
    }

    private boolean isAbnormal(MarkerResult result) {
        return !isNormal(result);
    }

    private String displayValue(MarkerResult result) {

        if (result.getNumericValue() != null) {

            String value = result.getNumericValue().stripTrailingZeros().toPlainString();

            String unit = result.getMarker().getUnit();

            return unit == null || unit.isBlank() ? value : value + " " + unit;
        }

        return result.getQualitativeValue().name().toLowerCase(Locale.ROOT);
    }

    private void appendSection(StringBuilder interpretation, String heading, List<String> statements
    ) {
        if (statements.isEmpty()) {
            return;
        }
        interpretation.append(heading).append("\n");

        for (String statement : statements) {
            interpretation.append("• ").append(statement).append("\n");
        }
        interpretation.append("\n");
    }
}
