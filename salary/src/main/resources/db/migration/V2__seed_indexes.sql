CREATE INDEX idx_employees_country_department ON employees (country_code, department);
CREATE INDEX idx_employees_manager_id ON employees (manager_id);
CREATE INDEX idx_salary_records_employee_effective_date ON salary_records (employee_id, effective_date);
