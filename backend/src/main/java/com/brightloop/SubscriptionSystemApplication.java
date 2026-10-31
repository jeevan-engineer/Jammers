package com.brightloop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Root application class.
 *
 * @EnableJpaAuditing — activates Spring Data JPA auditing so that
 * @CreatedDate and @LastModifiedDate in BaseEntity are automatically
 * populated on persist/merge without manual @PrePersist hooks.
 *
 * Component scan covers com.brightloop.** (all features live under
 * com.brightloop.subscription.** which is within this base package).
 */
@SpringBootApplication
@EnableJpaAuditing
public class SubscriptionSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubscriptionSystemApplication.class, args);
    }
}
