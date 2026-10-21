package com.example.ems.dao;

import com.example.ems.exception.DuplicateEmailException;
import com.example.ems.model.Employee;
import com.example.ems.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** All SQL lives here. Every query uses PreparedStatement. */
public class JdbcEmployeeDAO implements EmployeeDAO {
    private static final int MYSQL_DUPLICATE_ENTRY = 1062;

    private static final String SELECT_COLUMNS =
            "SELECT id, full_name, email, department, job_title, salary, hire_date, active FROM employees";

    @Override
    public Employee create(Employee e) throws SQLException {
        String sql = """
                INSERT INTO employees
                    (full_name, email, department, job_title, salary, hire_date)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getFullName());
            ps.setString(2, e.getEmail());
            ps.setString(3, e.getDepartment());
            ps.setString(4, e.getJobTitle());
            ps.setBigDecimal(5, e.getSalary());
            ps.setDate(6, Date.valueOf(e.getHireDate()));
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Creating employee failed.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    e.setId(keys.getLong(1));
                } else {
                    throw new SQLException("No generated employee ID returned.");
                }
            }
            return e;
        } catch (SQLException ex) {
            throw translate(ex, e.getEmail());
        }
    }

    @Override
    public Optional<Employee> findById(long id) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Employee> search(String name, String department, Boolean active,
                                 String sortBy, boolean ascending,
                                 int page, int pageSize) throws SQLException {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT_COLUMNS).append(" WHERE 1=1");
        appendFilters(sql, params, name, department, active);

        // Column name comes from a whitelist, never from raw user input.
        String column = switch (sortBy == null ? "" : sortBy.toLowerCase()) {
            case "hire_date" -> "hire_date";
            case "salary" -> "salary";
            default -> "full_name";
        };
        sql.append(" ORDER BY ").append(column).append(ascending ? " ASC" : " DESC")
           .append(", id ASC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((long) Math.max(page - 1, 0) * pageSize);

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Employee> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    @Override
    public long count(String name, String department, Boolean active) throws SQLException {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM employees WHERE 1=1");
        appendFilters(sql, params, name, department, active);
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    @Override
    public boolean update(Employee e) throws SQLException {
        String sql = """
                UPDATE employees
                SET full_name = ?, email = ?, department = ?, job_title = ?,
                    salary = ?, hire_date = ?
                WHERE id = ?
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getFullName());
            ps.setString(2, e.getEmail());
            ps.setString(3, e.getDepartment());
            ps.setString(4, e.getJobTitle());
            ps.setBigDecimal(5, e.getSalary());
            ps.setDate(6, Date.valueOf(e.getHireDate()));
            ps.setLong(7, e.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw translate(ex, e.getEmail());
        }
    }

    @Override
    public boolean deactivate(long id) throws SQLException {
        return setActive(id, false);
    }

    @Override
    public boolean reactivate(long id) throws SQLException {
        return setActive(id, true);
    }

    @Override
    public Map<String, Long> countByDepartment() throws SQLException {
        String sql = """
                SELECT department, COUNT(*) AS total FROM employees
                WHERE active = TRUE GROUP BY department ORDER BY department
                """;
        Map<String, Long> out = new LinkedHashMap<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.put(rs.getString("department"), rs.getLong("total"));
        }
        return out;
    }

    @Override
    public Map<String, BigDecimal> averageSalaryByDepartment() throws SQLException {
        String sql = """
                SELECT department, ROUND(AVG(salary), 2) AS avg_salary FROM employees
                WHERE active = TRUE GROUP BY department ORDER BY department
                """;
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.put(rs.getString("department"), rs.getBigDecimal("avg_salary"));
        }
        return out;
    }

    // ---------- helpers ----------

    private boolean setActive(long id, boolean active) throws SQLException {
        String sql = "UPDATE employees SET active = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setLong(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    private static void appendFilters(StringBuilder sql, List<Object> params,
                                      String name, String department, Boolean active) {
        if (name != null && !name.isBlank()) {
            sql.append(" AND full_name LIKE ?");
            params.add("%" + name.trim() + "%");
        }
        if (department != null && !department.isBlank()) {
            sql.append(" AND department = ?");
            params.add(department.trim());
        }
        if (active != null) {
            sql.append(" AND active = ?");
            params.add(active);
        }
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
    }

    private static Employee map(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setId(rs.getLong("id"));
        e.setFullName(rs.getString("full_name"));
        e.setEmail(rs.getString("email"));
        e.setDepartment(rs.getString("department"));
        e.setJobTitle(rs.getString("job_title"));
        e.setSalary(rs.getBigDecimal("salary"));
        e.setHireDate(rs.getDate("hire_date").toLocalDate());
        e.setActive(rs.getBoolean("active"));
        return e;
    }

    private static SQLException translate(SQLException ex, String email) {
        if (ex.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
            throw new DuplicateEmailException(email, ex);
        }
        return ex;
    }
}
