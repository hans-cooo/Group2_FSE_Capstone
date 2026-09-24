package com.group2.fse.ledger_service.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class AuditDbConfig {

    @Bean(name = "auditPostgresDataSource")
    public DataSource auditPostgresDataSource(
            @Value("${POSTGRES_PORT:5434}") String port,
            @Value("${POSTGRES_DB:audit_store}") String db,
            @Value("${POSTGRES_USER:postgres}") String user,
            @Value("${POSTGRES_PASSWORD:AuditPassword123!}") String password) {

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl("jdbc:postgresql://localhost:" + port + "/" + db);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);
        dataSource.setPoolName("HikariPool-PostgresAudit");

        return dataSource;
    }

    @Bean(name = "auditJdbcTemplate")
    public JdbcTemplate auditJdbcTemplate(DataSource auditPostgresDataSource) {
        return new JdbcTemplate(auditPostgresDataSource);
    }
}