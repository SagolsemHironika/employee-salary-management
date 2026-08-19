package com.acme.salary.seed;

import com.acme.salary.employee.Employee;
import com.acme.salary.employee.EmployeeRepository;
import com.acme.salary.salary.SalaryRecord;
import com.acme.salary.salary.SalaryRecordRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Generates a reproducible 10,000-employee dataset (fixed random seed) with a
 * realistic, non-uniform spread across country, department and band, plus
 * 1-3 chained salary_records per employee. Not wired into the default
 * profile — run with -Dspring-boot.run.profiles=seed.
 */
@Component
@Profile("seed")
public class SeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);
    private static final int EMPLOYEE_COUNT = 10_000;
    private static final int CHUNK_SIZE = 500;
    private static final long RANDOM_SEED = 42L;

    // Must mirror V3__seed_fx_rates.sql so seeded salaries and analytics agree.
    private static final List<CountryProfile> COUNTRIES = List.of(
            new CountryProfile("US", "USD", 0.25, 1.00000, 100),
            new CountryProfile("IN", "INR", 0.20, 0.01200, 1000),
            new CountryProfile("GB", "GBP", 0.12, 1.27000, 100),
            new CountryProfile("DE", "EUR", 0.10, 1.08000, 100),
            new CountryProfile("BR", "BRL", 0.10, 0.17000, 100),
            new CountryProfile("PL", "PLN", 0.08, 0.25000, 100),
            new CountryProfile("NG", "NGN", 0.08, 0.00062, 1000),
            new CountryProfile("JP", "JPY", 0.07, 0.00680, 1000));

    private static final List<DepartmentProfile> DEPARTMENTS = List.of(
            new DepartmentProfile("Engineering", "ENG", 0.30, Map.of(
                    1, "Associate Software Engineer", 2, "Software Engineer",
                    3, "Senior Software Engineer", 4, "Staff Software Engineer", 5, "Principal Engineer")),
            new DepartmentProfile("Sales", "SAL", 0.18, Map.of(
                    1, "Sales Development Rep", 2, "Account Executive",
                    3, "Senior Account Executive", 4, "Sales Manager", 5, "Sales Director")),
            new DepartmentProfile("Customer Support", "SUP", 0.15, Map.of(
                    1, "Support Associate", 2, "Support Specialist",
                    3, "Senior Support Specialist", 4, "Support Team Lead", 5, "Support Manager")),
            new DepartmentProfile("Operations", "OPS", 0.12, Map.of(
                    1, "Operations Associate", 2, "Operations Analyst",
                    3, "Senior Operations Analyst", 4, "Operations Manager", 5, "Director of Operations")),
            new DepartmentProfile("Product", "PRD", 0.08, Map.of(
                    1, "Associate Product Manager", 2, "Product Manager",
                    3, "Senior Product Manager", 4, "Group Product Manager", 5, "Director of Product")),
            new DepartmentProfile("Finance", "FIN", 0.07, Map.of(
                    1, "Finance Analyst", 2, "Senior Finance Analyst",
                    3, "Finance Manager", 4, "Senior Finance Manager", 5, "Director of Finance")),
            new DepartmentProfile("Marketing", "MKT", 0.06, Map.of(
                    1, "Marketing Associate", 2, "Marketing Specialist",
                    3, "Senior Marketing Manager", 4, "Marketing Director", 5, "VP of Marketing")),
            new DepartmentProfile("People", "PPL", 0.04, Map.of(
                    1, "HR Coordinator", 2, "HR Business Partner",
                    3, "Senior HR Business Partner", 4, "HR Manager", 5, "Director of People")));

    private static final Map<Integer, Double> LEVEL_WEIGHTS = Map.of(1, 0.15, 2, 0.35, 3, 0.30, 4, 0.15, 5, 0.05);
    private static final Map<Integer, int[]> LEVEL_USD_RANGE = Map.of(
            1, new int[] {45_000, 60_000},
            2, new int[] {60_000, 85_000},
            3, new int[] {85_000, 115_000},
            4, new int[] {115_000, 150_000},
            5, new int[] {150_000, 220_000});
    private static final List<String> RAISE_REASONS = List.of("annual_review", "promotion");

    private final EmployeeRepository employeeRepository;
    private final SalaryRecordRepository salaryRecordRepository;

    public SeedRunner(EmployeeRepository employeeRepository, SalaryRecordRepository salaryRecordRepository) {
        this.employeeRepository = employeeRepository;
        this.salaryRecordRepository = salaryRecordRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (employeeRepository.count() > 0) {
            log.info("Employees already present; skipping seed.");
            return;
        }

        log.info("Seeding {} employees...", EMPLOYEE_COUNT);
        Random random = new Random(RANDOM_SEED);
        Faker faker = new Faker(random);
        LocalDate today = LocalDate.now();

        List<Employee> chunk = new ArrayList<>(CHUNK_SIZE);
        List<int[]> chunkContext = new ArrayList<>(CHUNK_SIZE); // [level] parallel to chunk, country resolved via employee.countryCode
        int seeded = 0;

        for (int i = 1; i <= EMPLOYEE_COUNT; i++) {
            CountryProfile country = WeightedPicker.pick(COUNTRIES, CountryProfile::weight, random);
            DepartmentProfile department = WeightedPicker.pick(DEPARTMENTS, DepartmentProfile::weight, random);
            int level = pickLevel(random);
            LocalDate hireDate = randomDateBetween(today.minusYears(8), today, random);

            Employee employee = new Employee();
            employee.setEmployeeCode(String.format("EMP-%05d", i));
            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();
            employee.setFirstName(firstName);
            employee.setLastName(lastName);
            employee.setEmail(buildEmail(firstName, lastName, i));
            employee.setCountryCode(country.countryCode());
            employee.setDepartment(department.name());
            employee.setJobTitle(department.titlesByLevel().get(level));
            employee.setBand(department.bandPrefix() + "-L" + level);
            employee.setHireDate(hireDate);
            employee.setStatus(random.nextDouble() < 0.05 ? "terminated" : "active");

            chunk.add(employee);
            chunkContext.add(new int[] {level});

            if (chunk.size() == CHUNK_SIZE || i == EMPLOYEE_COUNT) {
                employeeRepository.saveAll(chunk);
                saveSalaryHistoryForChunk(chunk, chunkContext, random, today);
                seeded += chunk.size();
                log.info("Seeded {}/{} employees", seeded, EMPLOYEE_COUNT);
                chunk = new ArrayList<>(CHUNK_SIZE);
                chunkContext = new ArrayList<>(CHUNK_SIZE);
            }
        }

        log.info("Seed complete: {} employees, {} salary records", employeeRepository.count(), salaryRecordRepository.count());
    }

    private void saveSalaryHistoryForChunk(
            List<Employee> chunk, List<int[]> chunkContext, Random random, LocalDate today) {
        List<SalaryRecord> records = new ArrayList<>();
        for (int i = 0; i < chunk.size(); i++) {
            Employee employee = chunk.get(i);
            int level = chunkContext.get(i)[0];
            CountryProfile country = COUNTRIES.stream()
                    .filter(c -> c.countryCode().equals(employee.getCountryCode()))
                    .findFirst()
                    .orElseThrow();
            records.addAll(generateSalaryHistory(employee, country, level, random, today));
        }
        salaryRecordRepository.saveAll(records);
    }

    private List<SalaryRecord> generateSalaryHistory(
            Employee employee, CountryProfile country, int level, Random random, LocalDate today) {
        List<LocalDate> effectiveDates = new ArrayList<>();
        effectiveDates.add(employee.getHireDate());

        if (random.nextDouble() < 0.45 && employee.getHireDate().plusMonths(6).isBefore(today)) {
            LocalDate second = randomDateBetween(employee.getHireDate().plusMonths(6), today, random);
            effectiveDates.add(second);
            if (random.nextDouble() < 0.3 && second.plusMonths(6).isBefore(today)) {
                effectiveDates.add(randomDateBetween(second.plusMonths(6), today, random));
            }
        }

        int[] usdRange = LEVEL_USD_RANGE.get(level);
        double usdBase = usdRange[0] + random.nextDouble() * (usdRange[1] - usdRange[0]);

        List<SalaryRecord> history = new ArrayList<>(effectiveDates.size());
        for (int i = 0; i < effectiveDates.size(); i++) {
            if (i > 0) {
                usdBase *= 1.05 + random.nextDouble() * 0.15;
            }
            SalaryRecord record = new SalaryRecord();
            record.setEmployeeId(employee.getId());
            record.setCurrencyCode(country.currencyCode());
            record.setBaseSalary(toLocalCurrency(usdBase, country));
            record.setBonus(level >= 4 ? toLocalCurrency(usdBase * 0.08, country) : BigDecimal.ZERO);
            record.setAllowances(BigDecimal.ZERO);
            record.setEffectiveDate(effectiveDates.get(i));
            record.setEndDate(i + 1 < effectiveDates.size() ? effectiveDates.get(i + 1).minusDays(1) : null);
            record.setChangeReason(i == 0 ? "hire" : RAISE_REASONS.get(random.nextInt(RAISE_REASONS.size())));
            record.setCreatedBy("seed");
            history.add(record);
        }
        return history;
    }

    private static BigDecimal toLocalCurrency(double usdAmount, CountryProfile country) {
        double local = usdAmount / country.fxRateToUsd();
        double rounded = Math.round(local / country.roundingUnit()) * (double) country.roundingUnit();
        return BigDecimal.valueOf(rounded).setScale(2, RoundingMode.HALF_UP);
    }

    private static LocalDate randomDateBetween(LocalDate start, LocalDate end, Random random) {
        long days = ChronoUnit.DAYS.between(start, end);
        if (days <= 0) {
            return start;
        }
        return start.plusDays(random.nextLong(days + 1));
    }

    private static int pickLevel(Random random) {
        double target = random.nextDouble();
        double cumulative = 0;
        for (Map.Entry<Integer, Double> entry : LEVEL_WEIGHTS.entrySet()) {
            cumulative += entry.getValue();
            if (target <= cumulative) {
                return entry.getKey();
            }
        }
        return 3;
    }

    private static String buildEmail(String firstName, String lastName, int index) {
        String normalized = (firstName + "." + lastName)
                .toLowerCase()
                .replaceAll("[^a-z.]", "");
        return normalized + index + "@acme.example";
    }
}
