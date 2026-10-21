# Employee Management System — Java, JDBC & MySQL

Console application for managing employee records, built in layers:
`Main` (console) → `EmployeeService` (validation/rules) → `EmployeeDAO` (JDBC/SQL) → MySQL.

## Features
- Add, view (paged + sortable), find by ID, search by name, filter by department, update
- Soft delete (deactivate) and reactivate — records are never lost
- Reports: employee counts and average salary by department
- Validation (blank fields, email format, negative salary, future hire date)
- Clear handling of duplicate emails and missing records
- `PreparedStatement` everywhere; sort columns whitelisted
- SLF4J logging; JUnit 5 service tests with an in-memory fake DAO

## Setup
1. Install JDK 21 and MySQL Server.
2. Run `database/schema.sql` in MySQL Workbench (also inserts 4 sample rows).
3. Set environment variables (never commit them):
   - `DB_USER`, `DB_PASSWORD` (required)
   - `DB_URL` (optional, defaults to `jdbc:mysql://localhost:3306/employee_db...`)
   
   In IntelliJ: Run → Edit Configurations → Environment variables.
4. Run:
   ```bash
   mvn test            # unit tests (no database needed)
   mvn compile exec:java
   ```

## Project layout
```
database/schema.sql
src/main/java/com/example/ems/
  Main.java                      console menu
  model/Employee.java            data model (BigDecimal salary, LocalDate hire date)
  dao/EmployeeDAO.java           DAO contract
  dao/JdbcEmployeeDAO.java       all SQL
  service/EmployeeService.java   validation + business rules
  util/DBConnection.java         connection factory (env-based config)
  exception/                     NotFound, Validation, DuplicateEmail, DataAccess
src/test/java/.../service/       JUnit 5 tests
```

## Manual test checklist
See section 6 of the roadmap; all items are supported by the menu.

## Next steps (roadmap Phase 10)
Role-based login, audit log, CSV export, Docker Compose for MySQL, GitHub Actions CI,
then a Spring Boot REST API and web UI on top of the same service layer.
