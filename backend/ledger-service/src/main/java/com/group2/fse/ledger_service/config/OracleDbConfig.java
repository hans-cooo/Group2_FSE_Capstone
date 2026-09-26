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
            @Value("${spring.datasource.url:#{null}}") String explicitUrl,
            @Value("${ORACLE_HOST:${spring.datasource.host:localhost}}") String host,
            @Value("${ORACLE_PORT:${spring.datasource.port:1522}}") String port,
            @Value("${ORACLE_DATABASE:XEPDB1}") String db,
            @Value("${spring.datasource.username:${APP_USER:core_user}}") String user,
            @Value("${spring.datasource.password:${APP_USER_PASSWORD}}") String password) {

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
        String jdbcUrl = (explicitUrl != null && !explicitUrl.isBlank())
                ? explicitUrl
                : "jdbc:oracle:thin:@//" + host + ":" + port + "/" + db;
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(30);
        dataSource.setMinimumIdle(5);
        dataSource.setPoolName("HikariPool-OracleCore");

        return dataSource;
    }
}