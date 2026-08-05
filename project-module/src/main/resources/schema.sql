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

                         normal_min DECIMAL(10,2),
                         normal_max DECIMAL(10,2),

                         result_type VARCHAR(20) NOT NULL,

                         normal_qualitative_result VARCHAR(30),

                         CONSTRAINT valid_normal_range
                             CHECK (
                                 normal_min IS NULL
                                     OR normal_max IS NULL
                                     OR normal_min <= normal_max
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
                         report_date DATE NOT NULL
);


CREATE TABLE report_results (
                                result_id SERIAL PRIMARY KEY,

                                report_id INT NOT NULL
                                    REFERENCES reports(report_id)
                                        ON DELETE CASCADE,

                                marker_id INT NOT NULL
                                    REFERENCES markers(marker_id),

                                result_value DECIMAL(10,2),

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