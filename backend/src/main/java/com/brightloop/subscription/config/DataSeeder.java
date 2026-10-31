package com.brightloop.subscription.config;

import com.brightloop.subscription.common.enums.Role;
import com.brightloop.subscription.feature.customer.entity.Customer;
import com.brightloop.subscription.feature.customer.repository.CustomerRepository;
import com.brightloop.subscription.feature.user.entity.User;
import com.brightloop.subscription.feature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the database with initial users on application startup.
 *
 * CommandLineRunner — Spring Boot calls run() after the application context
 * is fully initialised but before the server starts accepting requests.
 * This ensures all JPA repositories and beans are ready.
 *
 * All seeding is idempotent: each user is only created if their email
 * is not already present in the users table. Safe to restart the server.
 *
 * @Transactional — all seed inserts in a single transaction; if any fail,
 * the whole seed operation is rolled back.
 *
 * SECURITY NOTE: Passwords are logged as a HINT (not the actual value)
 * to help the hackathon team get started without leaking credentials to logs.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedInternalStaff();
        seedDemoCustomer();
    }

    // ── Seed: ADMIN ───────────────────────────────────────────────────

    private void seedAdmin() {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("[DataSeeder] Admin user already exists — skipping: {}", adminEmail);
            return;
        }

        userRepository.save(User.builder()
                .fullName("System Administrator")
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .active(true)
                .build());

        // Log credential hint WITHOUT printing the actual password.
        log.info("[DataSeeder] ✅ Admin seeded | email={} | password=<see app.admin.password in properties>",
                adminEmail);
    }

    // ── Seed: INTERNAL staff ─────────────────────────────────────────

    private void seedInternalStaff() {
        final String staffEmail = "staff@jammers.com";
        if (userRepository.existsByEmail(staffEmail)) {
            log.info("[DataSeeder] Internal staff already exists — skipping: {}", staffEmail);
            return;
        }

        userRepository.save(User.builder()
                .fullName("Demo Staff Member")
                .email(staffEmail)
                // Hardcoded demo password — CHANGE before production.
                .passwordHash(passwordEncoder.encode("Staff@1234"))
                .role(Role.INTERNAL)
                .active(true)
                .build());

        log.info("[DataSeeder] ✅ Internal staff seeded | email={} | password=Staff@1234 (demo only)",
                staffEmail);
    }

    // ── Seed: CUSTOMER user + profile ────────────────────────────────

    private void seedDemoCustomer() {
        final String customerEmail = "customer@jammers.com";
        if (userRepository.existsByEmail(customerEmail)) {
            log.info("[DataSeeder] Demo customer already exists — skipping: {}", customerEmail);
            return;
        }

        // Create the user account.
        User customerUser = userRepository.save(User.builder()
                .fullName("Demo Customer")
                .email(customerEmail)
                // Hardcoded demo password — CHANGE before production.
                .passwordHash(passwordEncoder.encode("Customer@1234"))
                .role(Role.CUSTOMER)
                .active(true)
                .build());

        // Create the linked customer profile (mirrors what signup does).
        if (!customerRepository.existsByEmail(customerEmail)) {
            customerRepository.save(Customer.builder()
                    .user(customerUser)
                    .name("Demo Customer")
                    .email(customerEmail)
                    .companyName("Demo Corp")
                    .active(true)
                    .build());
        }

        log.info("[DataSeeder] ✅ Demo customer seeded | email={} | password=Customer@1234 (demo only)",
                customerEmail);
    }
}
