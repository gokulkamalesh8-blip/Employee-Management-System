package com.example.ems.service;

import com.example.ems.dao.EmployeeDAO;
import com.example.ems.exception.DuplicateEmailException;
import com.example.ems.model.Employee;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** Fake DAO so service tests never touch a real database. */
class InMemoryEmployeeDAO implements EmployeeDAO {
    private final Map<Long, Employee> store = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public Employee create(Employee e) {
        if (store.values().stream().anyMatch(x -> x.getEmail().equalsIgnoreCase(e.getEmail()))) {
            throw new DuplicateEmailException(e.getEmail());
        }
        e.setId(nextId++);
        store.put(e.getId(), e);
        return e;
    }

    @Override public Optional<Employee> findById(long id) { return Optional.ofNullable(store.get(id)); }

    @Override
    public List<Employee> search(String name, String dept, Boolean active, String sortBy,
                                 boolean asc, int page, int size) {
        return store.values().stream()
                .filter(e -> name == null || name.isBlank()
                        || e.getFullName().toLowerCase().contains(name.toLowerCase()))
                .filter(e -> dept == null || dept.isBlank() || e.getDepartment().equals(dept))
                .filter(e -> active == null || e.isActive() == active)
                .skip((long) (page - 1) * size).limit(size)
                .collect(Collectors.toList());
    }

    @Override public long count(String name, String dept, Boolean active) {
        return search(name, dept, active, null, true, 1, 1000).size();
    }

    @Override
    public boolean update(Employee e) {
        if (!store.containsKey(e.getId())) return false;
        store.put(e.getId(), e);
        return true;
    }

    @Override public boolean deactivate(long id) { return setActive(id, false); }
    @Override public boolean reactivate(long id) { return setActive(id, true); }

    private boolean setActive(long id, boolean a) {
        Employee e = store.get(id);
        if (e == null) return false;
        e.setActive(a);
        return true;
    }

    @Override public Map<String, Long> countByDepartment() {
        return store.values().stream().filter(Employee::isActive)
                .collect(Collectors.groupingBy(Employee::getDepartment, TreeMap::new, Collectors.counting()));
    }

    @Override public Map<String, BigDecimal> averageSalaryByDepartment() { return Map.of(); }
}
