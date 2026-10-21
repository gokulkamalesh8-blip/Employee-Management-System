package com.example.ems.dao;

import com.example.ems.model.Employee;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EmployeeDAO {
    Employee create(Employee employee) throws SQLException;

    Optional<Employee> findById(long id) throws SQLException;

    /**
     * Flexible search. Null/blank filters are ignored.
     * @param sortBy "name", "hire_date" or "salary" (anything else = name)
     * @param page   1-based page number
     */
    List<Employee> search(String name, String department, Boolean active,
                          String sortBy, boolean ascending,
                          int page, int pageSize) throws SQLException;

    long count(String name, String department, Boolean active) throws SQLException;

    boolean update(Employee employee) throws SQLException;

    boolean deactivate(long id) throws SQLException;

    boolean reactivate(long id) throws SQLException;

    Map<String, Long> countByDepartment() throws SQLException;

    Map<String, BigDecimal> averageSalaryByDepartment() throws SQLException;
}
