package com.group2.fse.auth_service.repository;

import com.group2.fse.auth_service.entity.Customer;
import com.group2.fse.auth_service.entity.Kyc;
import com.group2.fse.auth_service.entity.Role;
import com.group2.fse.auth_service.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("Phase 2: Auth JPA Entities & Repository Integration Tests")
class AuthRepositoryIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private KycRepository kycRepository;

    @Test
    @DisplayName("Should retrieve seed roles from Oracle XE")
    void shouldFindSeedRoles() {
        Optional<Role> adminRole = roleRepository.findByRoleName("ROLE_ADMIN");
        Optional<Role> tellerRole = roleRepository.findByRoleName("ROLE_TELLER");
        Optional<Role> customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER");

        assertThat(adminRole).isPresent();
        assertThat(adminRole.get().getRoleName()).isEqualTo("ROLE_ADMIN");

        assertThat(tellerRole).isPresent();
        assertThat(tellerRole.get().getRoleName()).isEqualTo("ROLE_TELLER");

        assertThat(customerRole).isPresent();
        assertThat(customerRole.get().getRoleName()).isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("Should retrieve seed internal staff users with mapped roles")
    void shouldFindSeedStaffUsers() {
        Optional<User> admin = userRepository.findByUsername("admin");
        assertThat(admin).isPresent();
        assertThat(admin.get().getEmail()).isEqualTo("admin@corebank.local");
        assertThat(admin.get().getRole().getRoleName()).isEqualTo("ROLE_ADMIN");
        assertThat(admin.get().getStatus()).isEqualTo("ACTIVE");

        Optional<User> tellerAlice = userRepository.findByUsername("teller_alice");
        assertThat(tellerAlice).isPresent();
        assertThat(tellerAlice.get().getRole().getRoleName()).isEqualTo("ROLE_TELLER");

        assertThat(userRepository.existsByUsername("admin")).isTrue();
        assertThat(userRepository.existsByEmail("admin@corebank.local")).isTrue();
        assertThat(userRepository.existsByUsername("non_existent_user")).isFalse();
    }

    @Test
    @DisplayName("Should retrieve seed retail customers with KYC status")
    void shouldFindSeedCustomers() {
        Optional<Customer> johnDoe = customerRepository.findByUsername("john_doe");
        assertThat(johnDoe).isPresent();
        assertThat(johnDoe.get().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(johnDoe.get().getKycStatus()).isEqualTo("VERIFIED");

        assertThat(customerRepository.existsByUsername("john_doe")).isTrue();
        assertThat(customerRepository.existsByEmail("john.doe@example.com")).isTrue();
        assertThat(customerRepository.existsByUsername("unknown_customer")).isFalse();
    }

    @Test
    @DisplayName("Should retrieve linked KYC profile for seed customer")
    void shouldFindKycByCustomerId() {
        Customer johnDoe = customerRepository.findByUsername("john_doe")
                .orElseThrow(() -> new AssertionError("john_doe seed customer must exist"));

        Optional<Kyc> kyc = kycRepository.findByCustomerCustomerId(johnDoe.getCustomerId());
        assertThat(kyc).isPresent();
        assertThat(kyc.get().getFirstName()).isEqualTo("John");
        assertThat(kyc.get().getLastName()).isEqualTo("Doe");
        assertThat(kyc.get().getStatus()).isEqualTo("VERIFIED");
    }

    @Test
    @DisplayName("Should persist and query newly registered customer")
    void shouldPersistAndQueryCustomer() {
        String uniqueUsername = "test_client_" + System.currentTimeMillis();
        String uniqueEmail = uniqueUsername + "@bank.local";

        Customer newCustomer = Customer.builder()
                .username(uniqueUsername)
                .email(uniqueEmail)
                .passwordHash("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi")
                .kycStatus("PENDING")
                .build();

        Customer saved = customerRepository.save(newCustomer);
        assertThat(saved.getCustomerId()).isNotNull();

        Optional<Customer> queried = customerRepository.findByUsername(uniqueUsername);
        assertThat(queried).isPresent();
        assertThat(queried.get().getEmail()).isEqualTo(uniqueEmail);
        assertThat(queried.get().getKycStatus()).isEqualTo("PENDING");
    }
}
