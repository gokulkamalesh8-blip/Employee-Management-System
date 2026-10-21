package com.example.ems.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Employee {
    private long id;
    private String fullName;
    private String email;
    private String department;
    private String jobTitle;
    private BigDecimal salary;
    private LocalDate hireDate;
    private boolean active = true;

    public Employee() {}

    public Employee(String fullName, String email, String department,
                    String jobTitle, BigDecimal salary, LocalDate hireDate) {
        this.fullName = fullName;
        this.email = email;
        this.department = department;
        this.jobTitle = jobTitle;
        this.salary = salary;
        this.hireDate = hireDate;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return String.format("%-5d %-22s %-28s %-14s %-20s %12s %-10s %s",
                id, fullName, email, department, jobTitle, salary, hireDate,
                active ? "Active" : "Inactive");
    }

    public static String header() {
        return String.format("%-5s %-22s %-28s %-14s %-20s %12s %-10s %s",
                "ID", "Name", "Email", "Department", "Job title", "Salary", "Hired", "Status");
    }
}
