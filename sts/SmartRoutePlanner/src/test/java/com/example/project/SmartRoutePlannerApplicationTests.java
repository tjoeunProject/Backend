package com.example.project;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import javax.sql.DataSource;

import com.example.project.member.domain.TravelUser;
import com.example.project.member.repository.TravelUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SmartRoutePlannerApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TravelUserRepository users;

    @Test
    void contextLoadsWithAnIsolatedInMemoryDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:");
        }
    }

    @Test
    void userRepositoryCanPersistAndFindATestUser() {
        TravelUser user = TravelUser.builder()
                .email("build-contract@example.invalid")
                .nickname("test user")
                .password("test-only-fixture")
                .build();
        users.saveAndFlush(user);

        assertThat(users.findByEmail("build-contract@example.invalid"))
                .isPresent()
                .get()
                .extracting(TravelUser::getId)
                .isEqualTo(user.getId());
    }

}
