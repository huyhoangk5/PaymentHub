package com.training.PaymentHub.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Đảm bảo bảng PMH_USER_PERMISSIONS và sequence tồn tại trong Oracle DB khi ddl-auto=none.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer tableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user_tables WHERE table_name = 'PMH_USER_PERMISSIONS'",
                    Integer.class
            );
            if (tableCount != null && tableCount == 0) {
                jdbcTemplate.execute(
                        "CREATE TABLE PMH_USER_PERMISSIONS (" +
                        "  ID NUMBER(19) PRIMARY KEY, " +
                        "  USER_ID NUMBER(19) NOT NULL, " +
                        "  PERMISSION_ID NUMBER(19) NOT NULL, " +
                        "  ACCESS_TYPE VARCHAR2(10) NOT NULL, " +
                        "  CONSTRAINT UK_PMH_USER_PERM UNIQUE (USER_ID, PERMISSION_ID), " +
                        "  CONSTRAINT FK_PMH_UP_USER FOREIGN KEY (USER_ID) REFERENCES PMH_USERS(ID) ON DELETE CASCADE, " +
                        "  CONSTRAINT FK_PMH_UP_PERM FOREIGN KEY (PERMISSION_ID) REFERENCES PMH_PERMISSIONS(ID) ON DELETE CASCADE" +
                        ")"
                );
                log.info("Created table PMH_USER_PERMISSIONS");
            }

            Integer seqCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user_sequences WHERE sequence_name = 'PMH_USER_PERMISSIONS_SEQ'",
                    Integer.class
            );
            if (seqCount != null && seqCount == 0) {
                jdbcTemplate.execute("CREATE SEQUENCE PMH_USER_PERMISSIONS_SEQ START WITH 1 INCREMENT BY 1 NOCACHE");
                log.info("Created sequence PMH_USER_PERMISSIONS_SEQ");
            }
        } catch (Exception e) {
            log.warn("Could not auto-initialize PMH_USER_PERMISSIONS schema: {}", e.getMessage());
        }
    }
}
