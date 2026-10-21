-- Dedicated database for integration tests. Never point tests at employee_db.
CREATE DATABASE IF NOT EXISTS employee_test_db;
USE employee_test_db;
SOURCE schema.sql;
