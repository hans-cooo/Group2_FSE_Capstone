package com.group2.fse.auth_service.repository;

import com.group2.fse.auth_service.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByUsername(String username);

    Optional<Customer> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
