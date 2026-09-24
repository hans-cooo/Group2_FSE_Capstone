package com.group2.fse.ledger_service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PostgresTestRunner implements CommandLineRunner {

    private final JdbcTemplate auditJdbcTemplate;

    public PostgresTestRunner(@Qualifier("auditJdbcTemplate") JdbcTemplate auditJdbcTemplate) {
        this.auditJdbcTemplate = auditJdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            // Standard ANSI SQL query to test active connection
            Integer result = auditJdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            if (result != null && result == 1) {
                System.out.println("\n=================================================");
                System.out.println(" SUCCESS: Both Oracle (JPA) and PostgreSQL (Audit) are connected!");
                System.out.println("=================================================\n");
            }
        } catch (Exception e) {
            System.err.println("\n=================================================");
            System.err.println(" FAILED: Could not query PostgreSQL audit store!");
            System.err.println(" Error: " + e.getMessage());
            System.err.println("=================================================\n");
        }
    }
}