package io.everyonecodes.spring_module.interpretations;

import io.everyonecodes.spring_module.model.MarkerResult;
import io.everyonecodes.spring_module.model.MarkerResultStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LiverReportInterpreter {

    /*
     * LIVER ENZYME EVALUATION ALGORITHM
     *
     * 1. Read ALT, AST, ALP and GGT results.
     *
     * 2. Calculate each enzyme's multiple of its upper limit:
     *    multiple of ULN = measured value / upper reference limit.
     *
     * 3. Determine the liver-enzyme pattern:
     *    - ALT normal and ALP normal:
     *        - AST high  -> isolated AST elevation.
     *        - AST normal -> no enzyme-elevation pattern.
     *    - ALT high and ALP normal -> hepatocellular pattern.
     *    - ALT normal and ALP high -> cholestatic pattern.
     *    - ALT high and ALP high   -> calculate the R-value.
     *
     * 4. Calculate the R-value when ALT and ALP are both elevated:
     *    R = (ALT / ALT upper limit) / (ALP / ALP upper limit).
     *
     *    - R >= 5        -> hepatocellular pattern.
     *    - R <= 2        -> cholestatic pattern.
     *    - R > 2 and < 5 -> mixed pattern.
     *
     * 5. Grade the ALT/AST elevation using the higher ULN multiple:
     *    - Above ULN but below 2 x ULN -> borderline elevation.
     *    - At least 2 x but below 5 x ULN -> mild elevation.
     *    - At least 5 x but up to 15 x ULN -> moderate elevation.
     *    - More than 15 x ULN -> severe elevation.
     *    - Raw ALT or AST above 10,000 U/L -> massive elevation.
     *
     * 6. Compare AST with ALT:
     *    - If both are high, calculate the AST-to-ALT ratio.
     *    - AST/ALT >= 2 may support an alcohol-associated pattern,
     *      but it does not establish a diagnosis.
     *    - Isolated AST elevation may have a non-liver source,
     *      particularly skeletal-muscle injury; consider CK.
     *
     * 7. Interpret ALP together with GGT:
     *    - ALP high and GGT high -> hepatobiliary origin is supported.
     *    - ALP high and GGT normal -> consider a non-liver source,
     *      especially bone.
     *    - GGT high and ALP normal -> isolated nonspecific GGT elevation.
     *    - ALP and GGT normal -> no bile-flow enzyme abnormality detected.
     */

    private static final BigDecimal R_VALUE_CHOLESTATIC_MAX = new BigDecimal("2");
    private static final BigDecimal R_VALUE_HEPATOCELLULAR_MIN = new BigDecimal("5");
    private static final BigDecimal TWO_TIMES_ULN = new BigDecimal("2");
    private static final BigDecimal THREE_TIMES_ULN = new BigDecimal("3");
    private static final BigDecimal FIVE_TIMES_ULN = new BigDecimal("5");
    private static final BigDecimal FIFTEEN_TIMES_ULN = new BigDecimal("15");
    private static final BigDecimal MASSIVE_TRANSAMINASE_VALUE = new BigDecimal("10000");
    private static final BigDecimal AST_ALT_RATIO_CLUE = new BigDecimal("2");
    private static final BigDecimal TWO_TIMES_BILIRUBIN_ULN = new BigDecimal("2");

    private static final Set<String> REQUIRED_MARKERS = Set.of(
            "ALT (GPT)",
            "AST (GOT)",
            "Alkaline Phosphatase (ALP)",
            "Gamma-GT (GGT)",
            "Total Bilirubin",
            "Albumin",
            "INR",
            "Cholinesterase (CHE)"
    );

    public String interpret(List<MarkerResult> results) {

        if (results.isEmpty()) {
            throw new IllegalArgumentException(
                    "Liver test results cannot be empty"
            );
        }

        Map<String, MarkerResult> resultsByName = results.stream()
                .collect(Collectors.toMap(
                        result -> result.getMarker().getName(),
                        result -> result
                ));

        if (!resultsByName.keySet().containsAll(REQUIRED_MARKERS)) {

            Set<String> missingMarkers = REQUIRED_MARKERS.stream()
                    .filter(marker -> !resultsByName.containsKey(marker))
                    .collect(Collectors.toSet());

            throw new IllegalStateException(
                    "Incorrect Liver Function Test configuration. "
                            + "Missing markers: "
                            + missingMarkers
            );
        }

        MarkerResult alt = resultsByName.get("ALT (GPT)");
        MarkerResult ast = resultsByName.get("AST (GOT)");
        MarkerResult alp = resultsByName.get("Alkaline Phosphatase (ALP)");
        MarkerResult ggt = resultsByName.get("Gamma-GT (GGT)");
        MarkerResult bilirubin = resultsByName.get("Total Bilirubin");
        MarkerResult albumin = resultsByName.get("Albumin");
        MarkerResult inr = resultsByName.get("INR");
        MarkerResult cholinesterase = resultsByName.get("Cholinesterase (CHE)");

        boolean altHigh = isHigh(alt);
        boolean astHigh = isHigh(ast);
        boolean alpHigh = isHigh(alp);
        boolean ggtHigh = isHigh(ggt);
        boolean bilirubinHigh = isHigh(bilirubin);

        boolean albuminLow = isLow(albumin);
        boolean inrHigh = isHigh(inr);
        boolean cholinesteraseLow = isLow(cholinesterase);

        boolean transaminasesHigh = altHigh || astHigh;
        boolean mainInjuryMarkersHigh = altHigh || astHigh || alpHigh;
        boolean anyHighLiverChemistry = mainInjuryMarkersHigh || ggtHigh || bilirubinHigh;

        boolean allResultsNormal = results.stream().allMatch(this::isNormal);

        BigDecimal altMultiple = multipleOfUpperLimit(alt);
        BigDecimal astMultiple = multipleOfUpperLimit(ast);
        BigDecimal alpMultiple = multipleOfUpperLimit(alp);
        BigDecimal bilirubinMultiple = multipleOfUpperLimit(bilirubin);

        BigDecimal highestTransaminaseMultiple = maximum(
                altMultiple,
                astMultiple
        );

        BigDecimal highestTransaminaseValue = maximum(
                alt.getNumericValue(),
                ast.getNumericValue()
        );

        InjuryPattern injuryPattern = determineInjuryPattern(
                alt,
                ast,
                alp
        );

        BigDecimal rValue = altHigh && alpHigh
                ? calculateRValue(alt, alp)
                : null;

        int reducedSynthesisMarkerCount = countTrue(
                albuminLow,
                inrHigh,
                cholinesteraseLow
        );

        boolean isolatedBilirubinElevation =
                bilirubinHigh
                        && isNormal(alt)
                        && isNormal(ast)
                        && isNormal(alp);

        /*
         * This is only a biochemical Hy's-law signal. A real Hy's-law
         * assessment additionally requires a medication relationship and
         * exclusion of obstruction and other causes.
         */
        boolean possibleHysLawSignal =
                highestTransaminaseMultiple.compareTo(THREE_TIMES_ULN) >= 0
                        && bilirubinMultiple.compareTo(TWO_TIMES_BILIRUBIN_ULN) >= 0
                        && alpMultiple.compareTo(TWO_TIMES_ULN) < 0;

        List<String> overallAssessment = new ArrayList<>();
        List<String> liverInjuryFindings = new ArrayList<>();
        List<String> bileFlowAndBilirubinFindings = new ArrayList<>();
        List<String> synthesisFindings = new ArrayList<>();
        List<String> otherFindings = new ArrayList<>();
        List<String> possibleInfluences = new ArrayList<>();


        // OVERALL ASSESSMENT


        if (allResultsNormal) {

            overallAssessment.add(
                    "No abnormalities were identified among the tested "
                            + "liver-injury, bile-flow, bilirubin and "
                            + "protein-production markers. Normal results "
                            + "do not exclude every liver condition."
            );

        } else {

            switch (injuryPattern) {
                case HEPATOCELLULAR -> overallAssessment.add(
                        "The main biochemical pattern is hepatocellular: "
                                + "ALT is elevated more disproportionately "
                                + "than alkaline phosphatase. This suggests "
                                + "liver-cell injury but does not identify "
                                + "its cause."
                );

                case CHOLESTATIC -> overallAssessment.add(
                        "The main biochemical pattern is cholestatic: "
                                + "alkaline phosphatase is elevated more "
                                + "disproportionately than ALT. This may "
                                + "involve bile formation or bile flow."
                );

                case MIXED -> overallAssessment.add(
                        "The results form a mixed liver-injury pattern, "
                                + "with both liver-cell and bile-flow "
                                + "markers contributing."
                );

                case ISOLATED_AST -> overallAssessment.add(
                        "AST is elevated without elevated ALT or alkaline "
                                + "phosphatase. AST can come from the liver, "
                                + "but also from skeletal muscle, heart and "
                                + "other tissues."
                );

                case NONE -> {
                    if (isolatedBilirubinElevation) {
                        overallAssessment.add(
                                "Total bilirubin is elevated while ALT, AST "
                                        + "and alkaline phosphatase are within "
                                        + "range. This is an isolated "
                                        + "hyperbilirubinemia pattern."
                        );

                    } else if (ggtHigh) {
                        overallAssessment.add(
                                "GGT is elevated without a classifiable ALT, "
                                        + "AST or alkaline-phosphatase injury "
                                        + "pattern. An isolated GGT elevation "
                                        + "is nonspecific."
                        );

                    } else if (!anyHighLiverChemistry) {
                        overallAssessment.add(
                                "There is no elevated liver-enzyme or "
                                        + "bilirubin pattern. One or more "
                                        + "lower-than-range or production-related "
                                        + "results are explained below."
                        );
                    }
                }
            }

            if (reducedSynthesisMarkerCount == 3) {

                overallAssessment.add(
                        "Albumin and cholinesterase are reduced while INR "
                                + "is elevated. Because all three production-"
                                + "related markers point in the same direction, "
                                + "this is a stronger pattern of possibly "
                                + "impaired liver synthetic function."
                );

            } else if (reducedSynthesisMarkerCount == 2) {

                overallAssessment.add(
                        "Two liver production-related markers are abnormal "
                                + "in the direction associated with reduced "
                                + "synthesis. Non-liver causes still need to "
                                + "be considered."
                );
            }
        }

        if (possibleHysLawSignal) {

            overallAssessment.add(
                    "ALT or AST is at least 3 times its upper limit, total "
                            + "bilirubin is at least 2 times its upper limit, "
                            + "and alkaline phosphatase is below 2 times its "
                            + "upper limit. This is a concerning biochemical "
                            + "signal used when assessing possible serious "
                            + "drug-related liver injury. It is not a diagnosis "
                            + "and requires prompt clinical assessment and "
                            + "exclusion of other causes."
            );
        }

        if (highestTransaminaseValue.compareTo(MASSIVE_TRANSAMINASE_VALUE) > 0) {

            overallAssessment.add(
                    "ALT or AST is above 10,000 U/L. This is a massive "
                            + "aminotransferase elevation and requires "
                            + "immediate medical evaluation."
            );

        } else if (
                transaminasesHigh && highestTransaminaseMultiple.compareTo(FIFTEEN_TIMES_ULN) > 0
        ) {

            overallAssessment.add(
                    "ALT or AST is more than 15 times its upper reference "
                            + "limit. This is a severe elevation and requires "
                            + "prompt medical evaluation."
            );

        } else if (
                transaminasesHigh
                        && highestTransaminaseMultiple.compareTo(FIVE_TIMES_ULN) >= 0
        ) {

            overallAssessment.add(
                    "ALT or AST is at least 5 times its upper reference "
                            + "limit. This is at least a moderate elevation "
                            + "and should be evaluated promptly."
            );
        }

        if (inrHigh && (mainInjuryMarkersHigh || bilirubinHigh)) {

            overallAssessment.add(
                    "An elevated INR appears together with liver-injury or "
                            + "bilirubin abnormalities. This combination may "
                            + "reflect impaired clotting-factor production, "
                            + "but anticoagulant medication, vitamin K status "
                            + "and other causes must be checked promptly."
            );
        }


        //  LIVER INJURY PATTERN



        if (transaminasesHigh) {

            String severity = transaminaseSeverity(
                    highestTransaminaseMultiple,
                    highestTransaminaseValue
            );

            if (altHigh && astHigh) {

                liverInjuryFindings.add(
                        "ALT is elevated at "
                                + displayValue(alt)
                                + " and AST is elevated at "
                                + displayValue(ast)
                                + ". The highest result represents a "
                                + severity
                                + " aminotransferase elevation relative to "
                                + "this laboratory's reference limits."
                );

                BigDecimal astAltRatio = ast.getNumericValue().divide(
                        alt.getNumericValue(),
                        2,
                        RoundingMode.HALF_UP
                );

                if (astAltRatio.compareTo(AST_ALT_RATIO_CLUE) >= 0) {

                    liverInjuryFindings.add(
                            "The AST-to-ALT ratio is "
                                    + formatDecimal(astAltRatio)
                                    + ". A ratio of 2 or more can occur in "
                                    + "alcohol-associated liver injury, but "
                                    + "the ratio alone cannot establish that "
                                    + "cause and can occur in other settings."
                    );

                } else {

                    liverInjuryFindings.add(
                            "The AST-to-ALT ratio is "
                                    + formatDecimal(astAltRatio)
                                    + ". It does not show the AST-to-ALT "
                                    + "ratio of 2 or more sometimes associated "
                                    + "with alcohol-related injury."
                    );
                }

            } else if (altHigh) {

                liverInjuryFindings.add(
                        "ALT is elevated at "
                                + displayValue(alt)
                                + ". ALT is more liver-specific than AST, "
                                + "so this supports possible liver-cell "
                                + "injury. The elevation is "
                                + severity
                                + " relative to the upper reference limit."
                );

            } else {

                liverInjuryFindings.add(
                        "AST is elevated at "
                                + displayValue(ast)
                                + " while ALT is not elevated. AST can rise "
                                + "from liver injury, but also from muscle, "
                                + "heart or other tissue injury. The elevation "
                                + "is "
                                + severity
                                + " relative to the upper reference limit."
                );
            }

        } else {

            liverInjuryFindings.add(
                    "ALT and AST are not elevated. The panel does not show "
                            + "an aminotransferase-elevation pattern."
            );
        }

        if (isLow(alt) && isLow(ast)) {

            liverInjuryFindings.add(
                    "ALT and AST are both below their reference ranges at "
                            + displayValue(alt)
                            + " and "
                            + displayValue(ast)
                            + ". Low aminotransferase values are not a typical "
                            + "sign of liver-cell injury."
            );

        } else {

            if (isLow(alt)) {
                liverInjuryFindings.add(
                        "ALT is below its reference range at "
                                + displayValue(alt)
                                + ". A low ALT is not a typical sign of "
                                + "liver-cell injury."
                );
            }

            if (isLow(ast)) {
                liverInjuryFindings.add(
                        "AST is below its reference range at "
                                + displayValue(ast)
                                + ". A low AST is generally not considered "
                                + "clinically important for liver injury."
                );
            }
        }

        if (rValue != null) {

            switch (injuryPattern) {
                case HEPATOCELLULAR -> liverInjuryFindings.add(
                        "The R-value is "
                                + formatDecimal(rValue)
                                + ". A value of 5 or more classifies this as "
                                + "a hepatocellular biochemical pattern."
                );

                case CHOLESTATIC -> liverInjuryFindings.add(
                        "The R-value is "
                                + formatDecimal(rValue)
                                + ". A value of 2 or less classifies this as "
                                + "a cholestatic biochemical pattern."
                );

                case MIXED -> liverInjuryFindings.add(
                        "The R-value is "
                                + formatDecimal(rValue)
                                + ". A value between 2 and 5 classifies this "
                                + "as a mixed biochemical pattern."
                );

                default -> {
                    // No R-value statement is needed for the remaining cases.
                }
            }
        }

        if (alpHigh && ggtHigh) {

            liverInjuryFindings.add(
                    "Alkaline phosphatase is elevated at "
                            + displayValue(alp)
                            + " and GGT is elevated at "
                            + displayValue(ggt)
                            + ". The elevated GGT supports a liver or bile-duct "
                            + "source for the alkaline-phosphatase elevation."
            );

        } else if (alpHigh) {

            liverInjuryFindings.add(
                    "Alkaline phosphatase is elevated at "
                            + displayValue(alp)
                            + " while GGT is not elevated. This does not "
                            + "support a liver source and raises the possibility "
                            + "of a non-hepatic source such as bone."
            );

        } else if (ggtHigh) {

            liverInjuryFindings.add(
                    "GGT is elevated at "
                            + displayValue(ggt)
                            + " while alkaline phosphatase is not elevated. "
                            + "An isolated GGT elevation is nonspecific and "
                            + "may be influenced by alcohol, medicines, "
                            + "supplements, smoking or metabolic conditions."
            );

        } else if (isNormal(alp) && isNormal(ggt)) {

            liverInjuryFindings.add(
                    "Alkaline phosphatase and GGT are within their expected "
                            + "ranges at "
                            + displayValue(alp)
                            + " and "
                            + displayValue(ggt)
                            + "."
            );
        }

        if (isLow(alp)) {

            if (transaminasesHigh) {

                liverInjuryFindings.add(
                        "Alkaline phosphatase is reduced at "
                                + displayValue(alp)
                                + " while ALT or AST is elevated. This is an "
                                + "unusual combination that may require targeted "
                                + "assessment; the panel alone cannot identify "
                                + "the cause."
                );

            } else {

                liverInjuryFindings.add(
                        "Alkaline phosphatase is reduced at "
                                + displayValue(alp)
                                + ". Low ALP is not a typical liver-injury "
                                + "pattern and may be associated with nutrition, "
                                + "mineral, thyroid or bone-related factors."
                );
            }
        }

        if (isLow(ggt)) {

            liverInjuryFindings.add(
                    "GGT is below its reference range at "
                            + displayValue(ggt)
                            + ". A low GGT is not a typical sign of liver or "
                            + "bile-duct injury."
            );
        }

        // BILE FLOW AND BILIRUBIN


        if (bilirubinHigh) {

            bileFlowAndBilirubinFindings.add(
                    "Total bilirubin is elevated at "
                            + displayValue(bilirubin)
                            + ". Total bilirubin alone cannot show whether the "
                            + "increase is conjugated (direct) or unconjugated "
                            + "(indirect); bilirubin fractionation is needed."
            );

            if (alpHigh && ggtHigh) {

                bileFlowAndBilirubinFindings.add(
                        "Elevated bilirubin appears together with elevated "
                                + "alkaline phosphatase and GGT. This strengthens "
                                + "the pattern of impaired bile flow or bilirubin "
                                + "excretion."
                );
            }

            if (transaminasesHigh) {

                bileFlowAndBilirubinFindings.add(
                        "Elevated bilirubin also appears with elevated ALT or "
                                + "AST. Bilirubin elevation can accompany "
                                + "hepatocellular or mixed liver injury."
                );
            }

            if (isolatedBilirubinElevation) {

                bileFlowAndBilirubinFindings.add(
                        "Because ALT, AST and alkaline phosphatase are within "
                                + "range, this is an isolated bilirubin "
                                + "elevation. Direct and indirect bilirubin are "
                                + "needed to distinguish reduced conjugation or "
                                + "increased red-cell breakdown from impaired "
                                + "hepatic excretion."
                );
            }

        } else if (isLow(bilirubin)) {

            bileFlowAndBilirubinFindings.add(
                    "Total bilirubin is below its reference range at "
                            + displayValue(bilirubin)
                            + ". A low bilirubin level is not a typical sign "
                            + "of liver dysfunction."
            );

        } else {

            bileFlowAndBilirubinFindings.add(
                    "Total bilirubin is within its expected range at "
                            + displayValue(bilirubin)
                            + "."
            );
        }


         // LIVER PROTEIN AND CLOTTING FUNCTION


        if (reducedSynthesisMarkerCount == 3) {

            synthesisFindings.add(
                    "Albumin and cholinesterase are low and INR is high. "
                            + "This combined pattern provides more support for "
                            + "possibly reduced hepatic protein and clotting-"
                            + "factor production than any single marker alone."
            );

        } else if (reducedSynthesisMarkerCount == 2) {

            synthesisFindings.add(
                    "Two of the three production-related markers point toward "
                            + "possibly reduced synthesis. The pattern is more "
                            + "meaningful than an isolated result, but it is "
                            + "still not specific to liver disease."
            );

        } else if (reducedSynthesisMarkerCount == 1) {

            synthesisFindings.add(
                    "Only one production-related marker points toward reduced "
                            + "synthesis. An isolated result is nonspecific and "
                            + "should be interpreted with its alternative "
                            + "influences."
            );

        } else if (
                isNormal(albumin)
                        && isNormal(inr)
                        && isNormal(cholinesterase)
        ) {

            synthesisFindings.add(
                    "Albumin, INR and cholinesterase are within their expected "
                            + "ranges. The tested markers do not show a reduced "
                            + "liver-synthesis pattern."
            );
        }

        if (albuminLow) {

            synthesisFindings.add(
                    "Albumin is reduced at "
                            + displayValue(albumin)
                            + ". Reduced liver production is one possibility, "
                            + "but inflammation, kidney protein loss, fluid "
                            + "overload and poor nutrition can also lower "
                            + "albumin. Albumin changes more slowly than INR."
            );

        } else if (isHigh(albumin)) {

            synthesisFindings.add(
                    "Albumin is elevated at "
                            + displayValue(albumin)
                            + ". This does not indicate increased liver "
                            + "function and is commonly associated with "
                            + "dehydration."
            );

        } else {

            synthesisFindings.add(
                    "Albumin is within its expected range at "
                            + displayValue(albumin)
                            + "."
            );
        }

        if (inrHigh) {

            synthesisFindings.add(
                    "INR is elevated at "
                            + displayValue(inr)
                            + ", meaning clot formation took longer than the "
                            + "laboratory reference. Possible explanations "
                            + "include reduced liver clotting-factor production, "
                            + "warfarin or another anticoagulant, vitamin K "
                            + "deficiency, or another coagulation disorder."
            );

        } else if (isLow(inr)) {

            synthesisFindings.add(
                    "INR is below its expected range at "
                            + displayValue(inr)
                            + ". This is not the usual direction caused by "
                            + "reduced liver clotting-factor production. Its "
                            + "meaning depends strongly on anticoagulant use "
                            + "and the reason for testing."
            );

        } else {

            synthesisFindings.add(
                    "INR is within its expected range at "
                            + displayValue(inr)
                            + "."
            );
        }

        if (cholinesteraseLow) {

            if (inrHigh || albuminLow) {

                synthesisFindings.add(
                        "Cholinesterase is reduced at "
                                + displayValue(cholinesterase)
                                + " and another production-related marker is "
                                + "also abnormal. This provides additional "
                                + "support for a possible reduced-synthesis "
                                + "pattern."
                );

            } else {

                synthesisFindings.add(
                        "Cholinesterase is reduced at "
                                + displayValue(cholinesterase)
                                + ". This may occur with impaired liver "
                                + "function or an inherited cholinesterase "
                                + "variant, but the isolated result is not "
                                + "diagnostic."
                );
            }

        } else if (isHigh(cholinesterase)) {

            synthesisFindings.add(
                    "Cholinesterase is elevated at "
                            + displayValue(cholinesterase)
                            + ". A high value has limited diagnostic meaning "
                            + "for liver function and may occur with diabetes, "
                            + "hypothyroidism or kidney protein loss."
            );

        } else {

            synthesisFindings.add(
                    "Cholinesterase is within its expected range at "
                            + displayValue(cholinesterase)
                            + "."
            );
        }



         // OTHER FINDINGS AND LIMITS


        if (astHigh && !altHigh) {

            otherFindings.add(
                    "Because AST is elevated without ALT, a creatine kinase "
                            + "(CK) result can help assess whether skeletal "
                            + "muscle contributed to the AST elevation."
            );
        }

        if (alpHigh && !ggtHigh) {

            otherFindings.add(
                    "An ALP isoenzyme test or assessment of bone-related "
                            + "markers may help clarify the source of the "
                            + "alkaline-phosphatase elevation."
            );
        }

        if (bilirubinHigh) {

            otherFindings.add(
                    "Direct and indirect bilirubin would add important "
                            + "information because total bilirubin alone cannot "
                            + "separate bilirubin-production, conjugation and "
                            + "excretion patterns."
            );
        }

        if (anyHighLiverChemistry) {

            otherFindings.add(
                    "A single panel describes the current biochemical pattern "
                            + "but cannot determine duration or cause. Previous "
                            + "results, repeat testing, symptoms, medication and "
                            + "supplement history, alcohol exposure and imaging "
                            + "may materially change the interpretation."
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
                        result.getMarker().getName()
                                + " ("
                                + displayValue(result)
                                + "): "
                                + influence
                );
            }
        }

        // BUILD THE FINAL REPORT


        StringBuilder interpretation = new StringBuilder();

        interpretation.append(
                "LIVER FUNCTION TEST – INTERPRETATION\n\n"
        );

        appendSection(
                interpretation,
                "OVERALL ASSESSMENT",
                overallAssessment
        );

        appendSection(
                interpretation,
                "LIVER INJURY PATTERN",
                liverInjuryFindings
        );

        appendSection(
                interpretation,
                "BILE FLOW AND BILIRUBIN",
                bileFlowAndBilirubinFindings
        );

        appendSection(
                interpretation,
                "LIVER PROTEIN AND CLOTTING FUNCTION",
                synthesisFindings
        );

        appendSection(
                interpretation,
                "OTHER FINDINGS",
                otherFindings
        );

        appendSection(
                interpretation,
                "POSSIBLE INFLUENCES ON ABNORMAL RESULTS",
                possibleInfluences
        );

        return interpretation.toString().trim();
    }

    private InjuryPattern determineInjuryPattern(
            MarkerResult alt,
            MarkerResult ast,
            MarkerResult alp
    ) {

        if (!isHigh(alt) && !isHigh(alp)) {
            return isHigh(ast)
                    ? InjuryPattern.ISOLATED_AST
                    : InjuryPattern.NONE;
        }

        if (isHigh(alt) && !isHigh(alp)) {
            return InjuryPattern.HEPATOCELLULAR;
        }

        if (!isHigh(alt)) {
            return InjuryPattern.CHOLESTATIC;
        }

        BigDecimal rValue = calculateRValue(alt, alp);

        if (rValue.compareTo(R_VALUE_HEPATOCELLULAR_MIN) >= 0) {
            return InjuryPattern.HEPATOCELLULAR;
        }

        if (rValue.compareTo(R_VALUE_CHOLESTATIC_MAX) <= 0) {
            return InjuryPattern.CHOLESTATIC;
        }

        return InjuryPattern.MIXED;
    }

    private BigDecimal calculateRValue(
            MarkerResult alt,
            MarkerResult alp
    ) {

        return multipleOfUpperLimit(alt).divide(
                multipleOfUpperLimit(alp),
                4,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal multipleOfUpperLimit(MarkerResult result) {

        return result.getNumericValue().divide(
                result.getMarker().getNormalMax(),
                4,
                RoundingMode.HALF_UP
        );
    }

    private String transaminaseSeverity(
            BigDecimal highestMultiple,
            BigDecimal highestValue
    ) {

        if (highestValue.compareTo(MASSIVE_TRANSAMINASE_VALUE) > 0) {
            return "massive";
        }

        if (highestMultiple.compareTo(FIFTEEN_TIMES_ULN) > 0) {
            return "severe";
        }

        if (highestMultiple.compareTo(FIVE_TIMES_ULN) >= 0) {
            return "moderate";
        }

        if (highestMultiple.compareTo(TWO_TIMES_ULN) >= 0) {
            return "mild";
        }

        return "borderline";
    }

    private int countTrue(boolean... values) {

        int count = 0;

        for (boolean value : values) {
            if (value) {
                count++;
            }
        }

        return count;
    }

    private BigDecimal maximum(
            BigDecimal first,
            BigDecimal second
    ) {

        return first.compareTo(second) >= 0
                ? first
                : second;
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

    private String displayValue(MarkerResult result) {

        if (result.getNumericValue() != null) {

            String value = result.getNumericValue()
                    .stripTrailingZeros()
                    .toPlainString();

            String unit = result.getMarker().getUnit();

            return unit == null || unit.isBlank()
                    ? value
                    : value + " " + unit;
        }

        return result.getQualitativeValue()
                .name()
                .toLowerCase(Locale.ROOT);
    }

    private String formatDecimal(BigDecimal value) {

        return value.setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private void appendSection(
            StringBuilder interpretation,
            String heading,
            List<String> statements
    ) {

        if (statements.isEmpty()) {
            return;
        }

        interpretation.append(heading)
                .append("\n");

        for (String statement : statements) {
            interpretation.append("• ")
                    .append(statement)
                    .append("\n");
        }

        interpretation.append("\n");
    }

    private enum InjuryPattern {
        NONE,
        HEPATOCELLULAR,
        CHOLESTATIC,
        MIXED,
        ISOLATED_AST
    }
}
