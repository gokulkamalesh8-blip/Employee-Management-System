package com.example.ems.service;

import com.example.ems.exception.DuplicateEmailException;
import com.example.ems.exception.EmployeeNotFoundException;
import com.example.ems.exception.ValidationException;
import com.example.ems.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeServiceTest {
    private EmployeeService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeService(new InMemoryEmployeeDAO());
    }

    private Employee valid() {
        return new Employee("Asha Raman", "Asha@Example.com", "Engineering",
                "Engineer", new BigDecimal("85000"), LocalDate.of(2022, 3, 14));
    }

    @Test
    void addsValidEmployeeAndNormalizesFields() {
        Employee saved = service.addEmployee(valid());
        assertTrue(saved.getId() > 0);
        assertEquals("asha@example.com", saved.getEmail());
        assertEquals(new BigDecimal("85000.00"), saved.getSalary());
    }

    @Test
    void rejectsBlankName() {
        Employee e = valid();
        e.setFullName("   ");
        assertThrows(ValidationException.class, () -> service.addEmployee(e));
    }

    @Test
    void rejectsInvalidEmail() {
        Employee e = valid();
        e.setEmail("not-an-email");
        assertThrows(ValidationException.class, () -> service.addEmployee(e));
    }

    @Test
    void rejectsNegativeSalary() {
        Employee e = valid();
        e.setSalary(new BigDecimal("-1"));
        assertThrows(ValidationException.class, () -> service.addEmployee(e));
    }

    @Test
    void rejectsFutureHireDate() {
        Employee e = valid();
        e.setHireDate(LocalDate.now().plusDays(1));
        assertThrows(ValidationException.class, () -> service.addEmployee(e));
    }

    @Test
    void rejectsDuplicateEmail() {
        service.addEmployee(valid());
        assertThrows(DuplicateEmailException.class, () -> service.addEmployee(valid()));
    }

    @Test
    void rejectsInvalidId() {
        assertThrows(ValidationException.class, () -> service.getById(0));
        assertThrows(ValidationException.class, () -> service.getById(-5));
    }

    @Test
    void findingMissingEmployeeThrowsNotFound() {
        assertThrows(EmployeeNotFoundException.class, () -> service.getById(999));
    }

    @Test
    void updatingMissingEmployeeThrowsNotFound() {
        Employee e = valid();
        e.setId(42);
        assertThrows(EmployeeNotFoundException.class, () -> service.updateEmployee(e));
    }

    @Test
    void updatesExistingEmployee() {
        Employee saved = service.addEmployee(valid());
        saved.setJobTitle("Senior Engineer");
        service.updateEmployee(saved);
        assertEquals("Senior Engineer", service.getById(saved.getId()).getJobTitle());
    }

    @Test
    void deactivateIsSoftDeleteAndReversible() {
        Employee saved = service.addEmployee(valid());
        service.deactivate(saved.getId());
        assertFalse(service.getById(saved.getId()).isActive());
        service.reactivate(saved.getId());
        assertTrue(service.getById(saved.getId()).isActive());
    }

    @Test
    void deactivatingMissingEmployeeThrowsNotFound() {
        assertThrows(EmployeeNotFoundException.class, () -> service.deactivate(123));
    }

    @Test
    void searchRejectsBadPaging() {
        assertThrows(ValidationException.class,
                () -> service.search(null, null, null, "name", true, 0, 10));
        assertThrows(ValidationException.class,
                () -> service.search(null, null, null, "name", true, 1, 500));
    }

    @Test
    void searchFindsByNameAndActiveStatus() {
        Employee saved = service.addEmployee(valid());
        assertEquals(1, service.search("asha", null, true, "name", true, 1, 10).size());
        service.deactivate(saved.getId());
        assertEquals(0, service.search("asha", null, true, "name", true, 1, 10).size());
        assertEquals(1, service.search("asha", null, false, "name", true, 1, 10).size());
    }
}
