CREATE DATABASE IF NOT EXISTS employee_db;
USE employee_db;

CREATE TABLE IF NOT EXISTS employees (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    department VARCHAR(80) NOT NULL,
    job_title VARCHAR(100) NOT NULL,
    salary DECIMAL(12, 2) NOT NULL,
    hire_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_employee_salary CHECK (salary >= 0)
);

CREATE INDEX idx_employees_name ON employees(full_name);
CREATE INDEX idx_employees_department ON employees(department);

-- Optional sample data
INSERT IGNORE INTO employees (full_name, email, department, job_title, salary, hire_date) VALUES
 ('Asha Raman',   'asha.raman@example.com',   'Engineering', 'Software Engineer', 85000.00, '2022-03-14'),
 ('Ravi Kumar',   'ravi.kumar@example.com',   'Engineering', 'Tech Lead',        120000.00, '2020-07-01'),
 ('Meena Iyer',   'meena.iyer@example.com',   'HR',          'HR Manager',        70000.00, '2021-01-10'),
 ('Karthik Das',  'karthik.das@example.com',  'Finance',     'Accountant',        60000.00, '2023-05-22');
