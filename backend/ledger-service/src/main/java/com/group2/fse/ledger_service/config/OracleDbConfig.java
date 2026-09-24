package com.group2.fse.ledger_service.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class OracleDbConfig {

    @Primary
    @Bean(name = "dataSource")
    public DataSource oracleDataSource(
            @Value("${ORACLE_PORT:1521}") String port,
            @Value("${ORACLE_DATABASE:XEPDB1}") String db,
            @Value("${APP_USER:core_user}") String user,
            @Value("${APP_USER_PASSWORD:CorePassword123!}") String password) {

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
        dataSource.setJdbcUrl("jdbc:oracle:thin:@localhost:" + port + "/" + db);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(30);
        dataSource.setMinimumIdle(5);
        dataSource.setPoolName("HikariPool-OracleCore");

        return dataSource;
    }
}