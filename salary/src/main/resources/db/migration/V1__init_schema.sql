CREATE TABLE employees (
    id             BIGSERIAL PRIMARY KEY,
    employee_code  VARCHAR(32)  NOT NULL UNIQUE,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    country_code   VARCHAR(2)   NOT NULL,
    department     VARCHAR(100) NOT NULL,
    job_title      VARCHAR(150) NOT NULL,
    band           VARCHAR(20)  NOT NULL,
    manager_id     BIGINT       REFERENCES employees (id),
    hire_date      DATE         NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'active',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_employees_status CHECK (status IN ('active', 'terminated'))
);

CREATE TABLE salary_records (
    id             BIGSERIAL PRIMARY KEY,
    employee_id    BIGINT        NOT NULL REFERENCES employees (id),
    base_salary    NUMERIC(14,2) NOT NULL,
    currency_code  VARCHAR(3)    NOT NULL,
    bonus          NUMERIC(14,2) NOT NULL DEFAULT 0,
    allowances     NUMERIC(14,2) NOT NULL DEFAULT 0,
    effective_date DATE          NOT NULL,
    end_date       DATE,
    change_reason  VARCHAR(30)   NOT NULL,
    created_by     VARCHAR(255)  NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_salary_records_change_reason
        CHECK (change_reason IN ('hire', 'promotion', 'annual_review', 'adjustment')),
    CONSTRAINT chk_salary_records_date_range
        CHECK (end_date IS NULL OR end_date >= effective_date)
);

CREATE TABLE fx_rates_snapshot (
    currency_code VARCHAR(3)    NOT NULL,
    rate_to_usd   NUMERIC(18,8) NOT NULL,
    as_of_date    DATE          NOT NULL,
    PRIMARY KEY (currency_code, as_of_date)
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'admin',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
