DROP TABLE IF EXISTS report_results CASCADE;
DROP TABLE IF EXISTS reports CASCADE;
DROP TABLE IF EXISTS test_type_markers CASCADE;
DROP TABLE IF EXISTS markers CASCADE;
DROP TABLE IF EXISTS test_types CASCADE;

CREATE TABLE test_types (
    test_type_id SERIAL PRIMARY KEY,
    test_name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    category VARCHAR(100)
);

CREATE TABLE markers (
    marker_id SERIAL PRIMARY KEY,
    marker_name VARCHAR(100) NOT NULL UNIQUE,
    unit VARCHAR(30) NOT NULL,
    normal_min DECIMAL(10,2) NOT NULL,
    normal_max DECIMAL(10,2) NOT NULL
);

CREATE TABLE test_type_markers (
    test_type_marker_id SERIAL PRIMARY KEY,

    test_type_id INT NOT NULL
        REFERENCES test_types(test_type_id),

    marker_id INT NOT NULL
        REFERENCES markers(marker_id),

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
        REFERENCES reports(report_id),

    marker_id INT NOT NULL
        REFERENCES markers(marker_id),

    result_value DECIMAL(10,2) NOT NULL,

    UNIQUE (report_id, marker_id)
);
