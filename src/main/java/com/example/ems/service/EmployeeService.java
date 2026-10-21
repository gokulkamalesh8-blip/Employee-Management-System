package com.example.ems.service;

import com.example.ems.dao.EmployeeDAO;
import com.example.ems.exception.DataAccessException;
import com.example.ems.exception.EmployeeNotFoundException;
import com.example.ems.exception.ValidationException;
import com.example.ems.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Validation and business rules. No SQL and no console I/O here. */
public class EmployeeService {
    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final BigDecimal MAX_SALARY = new BigDecimal("9999999999.99");

    private final EmployeeDAO dao;

    public EmployeeService(EmployeeDAO dao) { this.dao = dao; }

    public Employee addEmployee(Employee e) {
        validate(e);
        normalize(e);
        Employee saved = run("add employee", () -> dao.create(e));
        log.info("Created employee id={}", saved.getId());
        return saved;
    }

    public Employee getById(long id) {
        requireValidId(id);
        return run("find employee", () -> dao.findById(id))
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    public List<Employee> search(String name, String department, Boolean active,
                                 String sortBy, boolean ascending, int page, int pageSize) {
        if (page < 1) throw new ValidationException("Page must be 1 or greater.");
        if (pageSize < 1 || pageSize > 100) throw new ValidationException("Page size must be 1-100.");
        return run("search employees",
                () -> dao.search(name, department, active, sortBy, ascending, page, pageSize));
    }

    public long count(String name, String department, Boolean active) {
        return run("count employees", () -> dao.count(name, department, active));
    }

    public Employee updateEmployee(Employee e) {
        requireValidId(e.getId());
        validate(e);
        normalize(e);
        boolean updated = run("update employee", () -> dao.update(e));
        if (!updated) throw new EmployeeNotFoundException(e.getId());
        log.info("Updated employee id={}", e.getId());
        return e;
    }

    public void deactivate(long id) {
        requireValidId(id);
        if (!run("deactivate employee", () -> dao.deactivate(id))) {
            throw new EmployeeNotFoundException(id);
        }
        log.info("Deactivated employee id={}", id);
    }

    public void reactivate(long id) {
        requireValidId(id);
        if (!run("reactivate employee", () -> dao.reactivate(id))) {
            throw new EmployeeNotFoundException(id);
        }
        log.info("Reactivated employee id={}", id);
    }

    public Map<String, Long> countByDepartment() {
        return run("department counts", dao::countByDepartment);
    }

    public Map<String, BigDecimal> averageSalaryByDepartment() {
        return run("average salary", dao::averageSalaryByDepartment);
    }

    // ---------- validation ----------

    void validate(Employee e) {
        requireText(e.getFullName(), "Full name", 100);
        requireText(e.getEmail(), "Email", 150);
        requireText(e.getDepartment(), "Department", 80);
        requireText(e.getJobTitle(), "Job title", 100);
        if (!EMAIL.matcher(e.getEmail().trim()).matches()) {
            throw new ValidationException("Email format is invalid.");
        }
        if (e.getSalary() == null) throw new ValidationException("Salary is required.");
        if (e.getSalary().signum() < 0) throw new ValidationException("Salary cannot be negative.");
        if (e.getSalary().compareTo(MAX_SALARY) > 0) throw new ValidationException("Salary is too large.");
        if (e.getHireDate() == null) throw new ValidationException("Hire date is required.");
        if (e.getHireDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Hire date cannot be in the future.");
        }
    }

    private static void requireText(String value, String field, int max) {
        if (value == null || value.isBlank()) throw new ValidationException(field + " cannot be blank.");
        if (value.trim().length() > max) throw new ValidationException(field + " must be at most " + max + " characters.");
    }

    private static void requireValidId(long id) {
        if (id <= 0) throw new ValidationException("ID must be a positive number.");
    }

    private static void normalize(Employee e) {
        e.setFullName(e.getFullName().trim());
        e.setEmail(e.getEmail().trim().toLowerCase());
        e.setDepartment(e.getDepartment().trim());
        e.setJobTitle(e.getJobTitle().trim());
        e.setSalary(e.getSalary().setScale(2, java.math.RoundingMode.HALF_UP));
    }

    // ---------- SQLException -> DataAccessException ----------

    @FunctionalInterface
    private interface DbCall<T> { T call() throws SQLException; }

    private <T> T run(String action, DbCall<T> call) {
        try {
            return call.call();
        } catch (SQLException ex) {
            log.error("Database error during {}", action, ex);
            throw new DataAccessException("Database error while trying to " + action + ".", ex);
        }
    }
}
