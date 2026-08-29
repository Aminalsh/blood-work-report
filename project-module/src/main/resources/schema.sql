DROP TABLE IF EXISTS report_results CASCADE;
DROP TABLE IF EXISTS reports CASCADE;
DROP TABLE IF EXISTS test_type_markers CASCADE;
DROP TABLE IF EXISTS markers CASCADE;
DROP TABLE IF EXISTS test_types CASCADE;


CREATE TABLE test_types (
                            test_type_id SERIAL PRIMARY KEY,
                            test_name VARCHAR(100) NOT NULL UNIQUE,
                            description TEXT,
                            category VARCHAR(100)
);


CREATE TABLE markers (
                         marker_id SERIAL PRIMARY KEY,
                         marker_name VARCHAR(100) NOT NULL UNIQUE,

                         unit VARCHAR(30),

                         description TEXT NOT NULL,

                         normal_min DECIMAL(10,3),
                         normal_max DECIMAL(10,3),

                         result_type VARCHAR(20) NOT NULL,

                         normal_qualitative_result VARCHAR(30),

                         possible_influences TEXT,

                             CONSTRAINT valid_marker_configuration CHECK (
                                 (
                                     result_type = 'QUALITATIVE'
                                         AND normal_min IS NULL
                                         AND normal_max IS NULL
                                     )
                                     OR
                                 (
                                     result_type IN ('NUMERIC', 'MIXED')
                                         AND (
                                         normal_min IS NOT NULL
                                             OR normal_max IS NOT NULL
                                         )
                                         AND (
                                         normal_min IS NULL
                                             OR normal_max IS NULL
                                             OR normal_min <= normal_max
                                         )
                                     )
                                 )

);


CREATE TABLE test_type_markers (
                                   test_type_marker_id SERIAL PRIMARY KEY,

                                   test_type_id INT NOT NULL
                                       REFERENCES test_types(test_type_id)
                                           ON DELETE CASCADE,

                                   marker_id INT NOT NULL
                                       REFERENCES markers(marker_id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT unique_test_type_marker
                                       UNIQUE (test_type_id, marker_id)
);


CREATE TABLE reports (
                         report_id SERIAL PRIMARY KEY,

                         test_type_id INT NOT NULL
                             REFERENCES test_types(test_type_id),

                         report_code VARCHAR(50) NOT NULL UNIQUE,
                         report_date DATE NOT NULL,

                         interpretation TEXT NOT NULL
);

CREATE TABLE report_results (
                                result_id SERIAL PRIMARY KEY,

                                report_id INT NOT NULL
                                    REFERENCES reports(report_id)
                                        ON DELETE CASCADE,

                                marker_id INT NOT NULL
                                    REFERENCES markers(marker_id),

                                result_value DECIMAL(10,3),

                                qualitative_value VARCHAR(30),

                                status VARCHAR(20) NOT NULL,

                                CONSTRAINT unique_report_marker
                                    UNIQUE (report_id, marker_id),

                                CONSTRAINT exactly_one_result_value
                                    CHECK (
                                        (
                                            result_value IS NOT NULL
                                                AND qualitative_value IS NULL
                                            )
                                            OR
                                        (
                                            result_value IS NULL
                                                AND qualitative_value IS NOT NULL
                                            )
                                        )
);

INSERT INTO test_types (
    test_name,
    description,
    category
)
VALUES (
           'Kidney Function Test',
           'Evaluates kidney filtration, electrolyte balance and urine abnormalities using blood and urine markers.',
           'Kidney Health'
       )
ON CONFLICT (test_name)
    DO UPDATE SET
                  description = EXCLUDED.description,
                  category = EXCLUDED.category;

BEGIN;
INSERT INTO markers (
    marker_name,
    unit,
    normal_min,
    normal_max,
    description,
    result_type,
    normal_qualitative_result,
    possible_influences
)
VALUES
    (
        'Creatinine',
        'mg/dl',
        NULL,
        1.30,
        'Creatinine is a waste product removed from the blood by the kidneys. Elevated values may indicate reduced kidney filtration.',
        'NUMERIC',
        NULL,
        'Hydration status, recent intake of cooked meat, creatine supplementation, intense exercise, muscle mass, muscle injury, pregnancy, severe liver disease and certain medicines may influence creatinine. Relevant medicines include cimetidine, trimethoprim or cotrimoxazole, cobicistat, dolutegravir, ritonavir and fenofibrate. Hemolysis and elevated bilirubin may interfere with some measurement methods.'
    ),
    (
        'eGFR',
        'ml/min/1.73m²',
        90.00,
        NULL,
        'The estimated glomerular filtration rate estimates how effectively the kidneys filter the blood. Lower values may indicate reduced kidney function.',
        'NUMERIC',
        NULL,
        'Because eGFR is calculated from serum creatinine, factors that influence creatinine can also influence eGFR. These include cooked meat, creatine supplements, recent intense exercise, high or low muscle mass, pregnancy, amputation, muscle-wasting conditions, severe liver disease, dehydration and medicines that alter creatinine secretion. eGFR may also be less reliable when kidney function is changing rapidly.'
    ),
    (
        'Urea',
        'mg/dl',
        17.00,
        43.00,
        'Urea is a waste product produced during protein metabolism. It may increase with reduced kidney function or dehydration.',
        'NUMERIC',
        NULL,
        'Hydration status, high or low protein intake, gastrointestinal bleeding, fasting, malnutrition, fever, severe illness, tissue breakdown, recent surgery, liver function, pregnancy and overhydration may influence urea. Corticosteroids and certain antibiotics or other medicines may also affect the result.'
    ),
    (
        'Uric Acid',
        'mg/dl',
        3.60,
        7.20,
        'Uric acid is produced during the breakdown of purines. Elevated values may occur when the kidneys do not remove enough uric acid.',
        'NUMERIC',
        NULL,
        'Purine-rich foods such as organ meats, red meat, meat broth, some seafood and legumes may influence uric acid. Alcohol, high-fructose drinks, fasting, calorie-restricted diets, dehydration and rapid cell breakdown may also affect it. Relevant medicines include thiazide and loop diuretics, low-dose aspirin, niacin, pyrazinamide, ciclosporin, allopurinol and other urate-lowering medicines.'
    ),
    (
        'Sodium',
        'mmol/l',
        136.00,
        146.00,
        'Sodium is an electrolyte involved in fluid balance, nerve function and blood-pressure regulation.',
        'NUMERIC',
        NULL,
        'Fluid intake and hydration status strongly influence sodium. Vomiting, diarrhea, heavy sweating, intravenous fluids, very high blood glucose and changes in total body water may also affect the result. Diuretics, some antidepressants, some pain medicines and other medicines that influence fluid or hormone balance may alter sodium.'
    ),
    (
        'Potassium',
        'mmol/l',
        3.50,
        5.10,
        'Potassium is an electrolyte required for muscle, nerve and heart function. The kidneys remove excess potassium.',
        'NUMERIC',
        NULL,
        'Hemolysis, prolonged tourniquet use, repeated fist clenching and delayed separation of blood cells can produce an inaccurate potassium result. Potassium supplements, salt substitutes, very high potassium intake, natural licorice, diuretics, ACE inhibitors, angiotensin-receptor blockers, potassium-sparing medicines, laxatives, vomiting, diarrhea, insulin and some inhaled or injected medicines may influence potassium.'
    ),
    (
        'Chloride',
        'mmol/l',
        98.00,
        107.00,
        'Chloride helps regulate fluid balance and acid-base balance.',
        'NUMERIC',
        NULL,
        'Hydration status, vomiting, diarrhea, heavy sweating and changes in acid-base balance may influence chloride. Intravenous saline, diuretics, bicarbonate, citrate, antacids and medicines that alter fluid or electrolyte balance may also affect the result.'
    ),
    (
        'Calcium',
        'mmol/l',
        2.20,
        2.65,
        'Calcium is important for bones, muscles, nerves and blood clotting. Kidney function influences calcium metabolism.',
        'NUMERIC',
        NULL,
        'Total calcium is influenced by the serum albumin concentration. Prolonged tourniquet use and dehydration may produce an apparently elevated result. Calcium or vitamin D supplements, thiazide diuretics, lithium, antacids and medicines affecting bone or parathyroid metabolism may also influence calcium.'
    ),
    (
        'Phosphate',
        'mmol/l',
        0.81,
        1.45,
        'Phosphate is involved in bone health and cellular energy. The kidneys remove excess phosphate from the blood.',
        'NUMERIC',
        NULL,
        'Meal timing, fasting and time of day may influence phosphate. Hemolysis can produce a falsely elevated result. Vitamin D or phosphate supplements, phosphate-containing laxatives, enemas or infusions, diuretics, antacids, phosphate binders, insulin treatment and refeeding after malnutrition may also affect phosphate.'
    ),
    (
        'Magnesium',
        'mmol/l',
        0.66,
        1.07,
        'Magnesium supports muscle, nerve and enzyme function. The kidneys regulate magnesium balance.',
        'NUMERIC',
        NULL,
        'Magnesium supplements, magnesium-containing antacids or laxatives and magnesium infusions may influence the result. Diuretics, proton-pump inhibitors, cisplatin and other medicines may also affect magnesium. Vomiting, diarrhea, alcohol use, poor nutrition and sample hemolysis are additional possible influences.'
    ),
    (
        'Urine Protein',
        'mg/dl',
        0,
        15,
        'Detects protein in urine. Persistent protein in urine may indicate kidney damage.',
        'MIXED',
        'NEGATIVE',
        'Hydration status, fever, strenuous exercise, prolonged standing, pregnancy and temporary illness may influence urine protein. Concentrated or strongly alkaline urine, blood, urinary infection, semen or vaginal contamination, prolonged dipstick immersion, phenazopyridine, disinfectants and iodinated contrast may produce misleading dipstick results.'
    ),
    (
        'Urine Blood (Erythrocytes)',
        NULL,
        NULL,
        NULL,
        'Detects blood or erythrocytes in urine. Blood may occur with stones, infection, injury or kidney disease.',
        'QUALITATIVE',
        'NEGATIVE',
        'Menstruation, strenuous exercise, trauma and contamination during collection may influence urine blood results. Free hemoglobin or myoglobin can make the dipstick positive even without intact red blood cells. High urine specific gravity and vitamin C may cause a false-negative result. Delayed testing may also reduce reliability.'
    ),
    (
        'Urine Leukocytes',
        NULL,
        NULL,
        NULL,
        'Detects white blood cells in urine. A positive result may indicate inflammation or a urinary tract infection.',
        'QUALITATIVE',
        'NEGATIVE',
        'Contamination, vaginal secretions and incorrect collection may produce a misleading positive leukocyte result. Antibiotics, vitamin C and high concentrations of glucose, protein or other substances in urine may reduce test sensitivity. Delayed testing or incorrect sample storage may also influence the result.'
    ),
    (
        'Urine Nitrite',
        NULL,
        NULL,
        NULL,
        'Detects nitrite produced by certain bacteria. A positive result may suggest a bacterial urinary tract infection.',
        'QUALITATIVE',
        'NEGATIVE',
        'A short time between urinations, frequent urination, low dietary nitrate, dilute urine, antibiotics and vitamin C may cause a negative result despite infection. Some bacteria do not produce nitrite. Contamination, phenazopyridine, exposure of unused strips to air and delayed sample testing may also influence the result.'
    ),
    (
        'Urine pH',
        NULL,
        4.50,
        8.00,
        'Measures how acidic or alkaline the urine is.',
        'NUMERIC',
        NULL,
        'Diet, fasting, vomiting and the body acid-base status may influence urine pH. Urinary infections caused by certain bacteria may increase pH. Bicarbonate, citrate, antacids, acetazolamide and other medicines can also affect it. Delayed testing and bacterial growth in an old sample commonly make urine more alkaline.'
    ),
    (
        'Urine Specific Gravity',
        NULL,
        1.005,
        1.030,
        'Measures urine concentration and provides information about hydration and the kidneys ability to concentrate urine.',
        'NUMERIC',
        NULL,
        'Fluid intake, dehydration, vomiting, diarrhea, heavy sweating and diuretic medicines may influence urine concentration. Glucose, protein, intravenous contrast and other large dissolved substances may raise specific gravity. Results may also vary depending on whether a dipstick, refractometer or another measurement method is used.'
    ),
    (
        'Urine Glucose',
        'mg/dl',
        0,
        30,
        'Detects glucose in urine. Glucose should normally be absent.',
        'MIXED',
        'NEGATIVE',
        'Blood glucose level, pregnancy and the individual renal threshold for glucose may influence urine glucose. SGLT2-inhibitor medicines intentionally increase glucose excretion in urine. Vitamin C, high urine pH, high specific gravity and uric acid may reduce dipstick sensitivity. Bacteria and delayed testing can consume glucose and produce a falsely low or negative result.'
    ),
    (
        'Urine Ketones',
        'mg/dl',
        0,
        5,
        'Detects ketones in urine. Ketones should normally be absent.',
        'MIXED',
        'NEGATIVE',
        'Fasting, low-carbohydrate or ketogenic diets, prolonged exercise, vomiting, fever, pregnancy, alcohol use and poorly controlled diabetes may influence urine ketones. SGLT2-inhibitor medicines may also be relevant. High specific gravity, low urine pH, some medicine metabolites and delayed testing may alter the result. Some dipsticks do not detect every type of ketone equally.'
    )
ON CONFLICT (marker_name)
    DO UPDATE SET
        unit = EXCLUDED.unit,
        normal_min = EXCLUDED.normal_min,
        normal_max = EXCLUDED.normal_max,
        description = EXCLUDED.description,
        result_type = EXCLUDED.result_type,
        possible_influences = EXCLUDED.possible_influences,
        normal_qualitative_result =
        EXCLUDED.normal_qualitative_result;

COMMIT;

INSERT INTO test_type_markers (
    test_type_id,
    marker_id
)
SELECT
    tt.test_type_id,
    m.marker_id
FROM test_types tt
         CROSS JOIN markers m
WHERE tt.test_name = 'Kidney Function Test'
  AND m.marker_name IN (
                        'Creatinine',
                        'eGFR',
                        'Urea',
                        'Uric Acid',
                        'Sodium',
                        'Potassium',
                        'Chloride',
                        'Calcium',
                        'Phosphate',
                        'Magnesium',
                        'Urine Protein',
                        'Urine Blood (Erythrocytes)',
                        'Urine Leukocytes',
                        'Urine Nitrite',
                        'Urine pH',
                        'Urine Specific Gravity',
                        'Urine Glucose',
                        'Urine Ketones'
    )
ON CONFLICT (test_type_id, marker_id)
    DO NOTHING;
INSERT INTO test_types (test_name, description, category)
VALUES (
           'Liver Function Test',
           'Evaluates liver-cell injury, bile-flow markers, bilirubin and hepatic protein/clotting synthesis.',
           'Liver'
       )
ON CONFLICT (test_name) DO UPDATE
    SET description = EXCLUDED.description,
        category = EXCLUDED.category;


-- -------------------------------------------------------------
-- 2. Create or update all markers required by
--    LiverReportInterpreter.REQUIRED_MARKERS
-- -------------------------------------------------------------
INSERT INTO markers (
    marker_name,
    unit,
    normal_min,
    normal_max,
    description,
    possible_influences,
    result_type
)
VALUES
    (
        'ALT (GPT)',
        'U/L',
        10.00,
        45.00,
        'Alanine aminotransferase is an enzyme found mainly in liver cells. An increased activity supports a hepatocellular-injury pattern but does not identify the cause.',
        'Higher values may occur with metabolic fatty liver disease, viral or autoimmune hepatitis, alcohol-related or medication/supplement-related injury, reduced liver blood flow, or strenuous exercise. Hemolysis and sample handling can affect the result. Low values are usually not a sign of liver injury.',
        'NUMERIC'
    ),
    (
        'AST (GOT)',
        'U/L',
        10.00,
        35.00,
        'Aspartate aminotransferase occurs in liver cells but also in skeletal muscle, heart and blood cells. It is interpreted together with ALT.',
        'Higher values may reflect liver injury, strenuous exercise, skeletal-muscle injury, heart injury, hemolysis, intramuscular injections, alcohol exposure, or medicines/supplements. A hemolyzed sample can falsely increase AST. Low values are generally not clinically important for liver injury.',
        'NUMERIC'
    ),
    (
        'Alkaline Phosphatase (ALP)',
        'U/L',
        30.00,
        129.00,
        'Alkaline phosphatase is produced mainly by bile-duct and bone tissue. A high result is interpreted with GGT to help assess whether the source is hepatobiliary.',
        'Higher values may occur with impaired bile flow, bile-duct disease, bone growth or bone turnover, healing fractures, pregnancy, or medicines. Lower values may occur with poor nutrition, zinc or magnesium deficiency, hypothyroidism, severe illness, or the rare disorder hypophosphatasia.',
        'NUMERIC'
    ),
    (
        'Gamma-GT (GGT)',
        'U/L',
        12.00,
        55.00,
        'Gamma-glutamyl transferase is a sensitive but nonspecific liver and bile-duct enzyme. When ALP is also elevated, GGT supports a hepatobiliary source of the ALP elevation.',
        'Higher values may occur with impaired bile flow, chronic alcohol exposure, smoking, obesity or metabolic fatty liver disease, and enzyme-inducing medicines or supplements. Hemolysis can produce a falsely lower measured activity. A low value is usually not clinically important.',
        'NUMERIC'
    ),
    (
        'Total Bilirubin',
        'mg/dL',
        0.00,
        1.00,
        'Total bilirubin combines conjugated and unconjugated bilirubin. An elevated total result should be fractionated into direct and indirect bilirubin to clarify the biochemical pattern.',
        'Higher values may occur with increased red-cell breakdown, Gilbert syndrome, fasting or intercurrent illness, liver-cell injury, impaired bile flow, or medicines. Light exposure can lower bilirubin in the sample, and hemolysis may interfere with measurement. Low values are generally not clinically important.',
        'NUMERIC'
    ),
    (
        'Albumin',
        'g/dL',
        3.40,
        4.80,
        'Albumin is a protein made by the liver. Because it changes slowly and has several non-liver influences, it is interpreted with INR and cholinesterase rather than alone.',
        'Lower values may occur with reduced hepatic synthesis, inflammation, kidney or gastrointestinal protein loss, malnutrition, pregnancy, fluid overload, or severe illness. Higher values usually reflect dehydration or prolonged venous stasis during blood collection.',
        'NUMERIC'
    ),
    (
        'INR',
        NULL,
        0.85,
        1.15,
        'The international normalized ratio standardizes prothrombin time and reflects clotting-factor activity. In a person not taking anticoagulants, an increased INR can support reduced hepatic clotting-factor synthesis.',
        'Higher values may occur with vitamin K antagonist treatment such as warfarin, vitamin K deficiency or malabsorption, cholestasis, reduced liver clotting-factor synthesis, inherited or acquired factor deficiency, disseminated coagulation disorders, or specimen problems. Interpretation requires the medication history and testing indication.',
        'NUMERIC'
    ),
    (
        'Cholinesterase (CHE)',
        'U/L',
        5400.00,
        13200.00,
        'Serum cholinesterase is produced mainly by the liver. A reduced activity can support impaired hepatic synthesis when albumin or INR is abnormal in the same direction.',
        'Lower values may occur with reduced liver synthesis, inherited butyrylcholinesterase variants, organophosphate exposure or cholinesterase-inhibiting medicines, malnutrition or malabsorption, pregnancy, inflammation, or severe illness. Higher values have limited liver-specific meaning and may occur with diabetes, hypothyroidism, obesity, hyperlipidemia, or kidney protein loss.',
        'NUMERIC'
    )
ON CONFLICT (marker_name) DO UPDATE
    SET unit = EXCLUDED.unit,
        normal_min = EXCLUDED.normal_min,
        normal_max = EXCLUDED.normal_max,
        description = EXCLUDED.description,
        possible_influences = EXCLUDED.possible_influences,
        result_type = EXCLUDED.result_type;


-- -------------------------------------------------------------
-- 3. Connect the eight markers to Liver Function Test
-- -------------------------------------------------------------
INSERT INTO test_type_markers (test_type_id, marker_id)
SELECT test_type.test_type_id, marker.marker_id
FROM test_types AS test_type
         CROSS JOIN markers AS marker
WHERE test_type.test_name = 'Liver Function Test'
  AND marker.marker_name IN (
                             'ALT (GPT)',
                             'AST (GOT)',
                             'Alkaline Phosphatase (ALP)',
                             'Gamma-GT (GGT)',
                             'Total Bilirubin',
                             'Albumin',
                             'INR',
                             'Cholinesterase (CHE)'
    )
ON CONFLICT (test_type_id, marker_id) DO NOTHING;


-- -------------------------------------------------------------
-- OPTIONAL: use this block INSTEAD when the database is intended
-- exclusively for an adult female reference profile.
-- Do not run it for male reports.
-- -------------------------------------------------------------
-- UPDATE markers
-- SET normal_min = 10.00, normal_max = 34.00
-- WHERE marker_name = 'ALT (GPT)';
--
-- UPDATE markers
-- SET normal_min = 10.00, normal_max = 31.00
-- WHERE marker_name = 'AST (GOT)';
--
-- UPDATE markers
-- SET normal_min = 30.00, normal_max = 104.00
-- WHERE marker_name = 'Alkaline Phosphatase (ALP)';
--
-- UPDATE markers
-- SET normal_min = 9.00, normal_max = 38.00
-- WHERE marker_name = 'Gamma-GT (GGT)';


-- -------------------------------------------------------------
-- 4. Verification: this query must return exactly eight rows.
-- -------------------------------------------------------------
SELECT
    marker.marker_name,
    marker.unit,
    marker.normal_min,
    marker.normal_max,
    marker.result_type
FROM test_types AS test_type
         JOIN test_type_markers AS connection
              ON connection.test_type_id = test_type.test_type_id
         JOIN markers AS marker
              ON marker.marker_id = connection.marker_id
WHERE test_type.test_name = 'Liver Function Test'
ORDER BY marker.marker_name;
