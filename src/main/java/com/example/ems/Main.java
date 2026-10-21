package com.example.ems;

import com.example.ems.dao.JdbcEmployeeDAO;
import com.example.ems.exception.DataAccessException;
import com.example.ems.exception.DuplicateEmailException;
import com.example.ems.exception.EmployeeNotFoundException;
import com.example.ems.exception.ValidationException;
import com.example.ems.model.Employee;
import com.example.ems.service.EmployeeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/** Console menu: prompts and messages only. No SQL here. */
public class Main {
    private static final int PAGE_SIZE = 10;

    private final Scanner in = new Scanner(System.in);
    private final EmployeeService service = new EmployeeService(new JdbcEmployeeDAO());

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Choose an option");
            try {
                switch (choice) {
                    case "1" -> addEmployee();
                    case "2" -> viewAll();
                    case "3" -> findById();
                    case "4" -> searchByName();
                    case "5" -> filterByDepartment();
                    case "6" -> updateEmployee();
                    case "7" -> deactivate();
                    case "8" -> showCount();
                    case "9" -> showAverageSalary();
                    case "10" -> reactivate();
                    case "0" -> running = false;
                    default -> System.out.println("Invalid option. Enter a number from the menu.");
                }
            } catch (ValidationException | DuplicateEmailException | EmployeeNotFoundException e) {
                System.out.println("✗ " + e.getMessage());
            } catch (DataAccessException e) {
                System.out.println("✗ " + e.getMessage() + " Check the log and your DB settings.");
            } catch (IllegalStateException e) {
                System.out.println("✗ Configuration problem: " + e.getMessage());
            }
        }
        System.out.println("Goodbye!");
    }

    private void printMenu() {
        System.out.println("""

                ========== EMPLOYEE MANAGEMENT SYSTEM ==========
                 1. Add employee
                 2. View all employees (paged, sortable)
                 3. Find employee by ID
                 4. Search employees by name
                 5. Filter by department
                 6. Update employee
                 7. Deactivate employee
                 8. Show employee count
                 9. Show average salary by department
                10. Reactivate employee
                 0. Exit""");
    }

    // ---------- actions ----------

    private void addEmployee() {
        Employee e = new Employee(
                prompt("Full name"),
                prompt("Email"),
                prompt("Department"),
                prompt("Job title"),
                readSalary("Salary"),
                readDate("Hire date (yyyy-MM-dd)"));
        Employee saved = service.addEmployee(e);
        System.out.println("✓ Employee added with ID " + saved.getId());
    }

    private void viewAll() {
        System.out.println("Sort by: name / hire_date / salary (Enter = name)");
        String sort = prompt("Sort by");
        String dir = prompt("Descending? (y/N)");
        boolean asc = !dir.equalsIgnoreCase("y");
        String status = prompt("Show: (a)ctive, (i)nactive, or (b)oth? [a]");
        Boolean active = status.equalsIgnoreCase("b") ? null : !status.equalsIgnoreCase("i");

        int page = 1;
        while (true) {
            long total = service.count(null, null, active);
            long pages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
            List<Employee> rows = service.search(null, null, active, sort, asc, page, PAGE_SIZE);
            printTable(rows);
            System.out.printf("Page %d of %d (%d employees)%n", page, pages, total);
            String nav = prompt("[n]ext, [p]revious, [q]uit to menu").toLowerCase();
            if (nav.equals("n") && page < pages) page++;
            else if (nav.equals("p") && page > 1) page--;
            else if (nav.equals("q") || nav.isEmpty()) return;
        }
    }

    private void findById() {
        long id = readLong("Employee ID");
        Employee e = service.getById(id);
        System.out.println(Employee.header());
        System.out.println(e);
    }

    private void searchByName() {
        String name = prompt("Name contains");
        printTable(service.search(name, null, null, "name", true, 1, 100));
    }

    private void filterByDepartment() {
        String dept = prompt("Department (exact name)");
        printTable(service.search(null, dept, null, "name", true, 1, 100));
    }

    private void updateEmployee() {
        long id = readLong("Employee ID to update");
        Employee e = service.getById(id);
        System.out.println("Press Enter to keep the current value.");
        e.setFullName(keep("Full name", e.getFullName()));
        e.setEmail(keep("Email", e.getEmail()));
        e.setDepartment(keep("Department", e.getDepartment()));
        e.setJobTitle(keep("Job title", e.getJobTitle()));
        String sal = prompt("Salary [" + e.getSalary() + "]");
        if (!sal.isBlank()) e.setSalary(parseSalary(sal));
        String date = prompt("Hire date [" + e.getHireDate() + "]");
        if (!date.isBlank()) e.setHireDate(parseDate(date));
        service.updateEmployee(e);
        System.out.println("✓ Employee " + id + " updated.");
    }

    private void deactivate() {
        long id = readLong("Employee ID to deactivate");
        Employee e = service.getById(id);
        String confirm = prompt("Deactivate " + e.getFullName() + "? (y/N)");
        if (confirm.equalsIgnoreCase("y")) {
            service.deactivate(id);
            System.out.println("✓ Employee deactivated (record kept, can be reactivated).");
        } else {
            System.out.println("Cancelled.");
        }
    }

    private void reactivate() {
        service.reactivate(readLong("Employee ID to reactivate"));
        System.out.println("✓ Employee reactivated.");
    }

    private void showCount() {
        System.out.println("Active employees : " + service.count(null, null, true));
        System.out.println("Inactive         : " + service.count(null, null, false));
        System.out.println("Active by department:");
        for (Map.Entry<String, Long> en : service.countByDepartment().entrySet()) {
            System.out.printf("  %-18s %d%n", en.getKey(), en.getValue());
        }
    }

    private void showAverageSalary() {
        System.out.println("Average salary by department (active employees):");
        for (Map.Entry<String, BigDecimal> en : service.averageSalaryByDepartment().entrySet()) {
            System.out.printf("  %-18s %12s%n", en.getKey(), en.getValue());
        }
    }

    // ---------- input helpers ----------

    private String prompt(String label) {
        System.out.print(label + ": ");
        return in.hasNextLine() ? in.nextLine().trim() : "0"; // EOF -> exit cleanly
    }

    private String keep(String label, String current) {
        String v = prompt(label + " [" + current + "]");
        return v.isBlank() ? current : v;
    }

    private long readLong(String label) {
        while (true) {
            try {
                return Long.parseLong(prompt(label));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private BigDecimal readSalary(String label) {
        while (true) {
            try {
                return parseSalary(prompt(label));
            } catch (ValidationException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private LocalDate readDate(String label) {
        while (true) {
            try {
                return parseDate(prompt(label));
            } catch (ValidationException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static BigDecimal parseSalary(String text) {
        try {
            return new BigDecimal(text.replace(",", ""));
        } catch (NumberFormatException e) {
            throw new ValidationException("Salary must be a number, e.g. 55000.00");
        }
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Date must look like 2024-01-31.");
        }
    }

    private static void printTable(List<Employee> rows) {
        if (rows.isEmpty()) {
            System.out.println("No employees found.");
            return;
        }
        System.out.println(Employee.header());
        rows.forEach(System.out::println);
    }
}
