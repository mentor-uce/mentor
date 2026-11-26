package com.mentor;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import({PostgreSqlTestConfiguration.class, MongoDbTestConfiguration.class, RedisTestConfiguration.class, JwtTestConfiguration.class})
class MentorApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void startupAppliesVersionOneMigrationSuccessfully() throws Exception {
        var dataSource = context.getBean(DataSource.class);
        try (var connection = dataSource.getConnection();
                var statement = connection.createStatement();
                var result = statement.executeQuery("""
                        SELECT version, script, success, checksum
                        FROM flyway_schema_history
                        WHERE version = '1'
                        """)) {
            assertTrue(result.next(), "V1 must have run before the application is ready");
            assertEquals("1", result.getString("version"));
            assertEquals("V1__init.sql", result.getString("script"));
            assertTrue(result.getBoolean("success"));
            assertNotNull(result.getObject("checksum"), "Flyway must track SQL integrity");
            assertFalse(result.next(), "V1 must only have one history record");
        }
    }

    @Test
    void applicationConnectsToPostgreSqlOnStartup() throws Exception {
        var dataSource = context.getBean(DataSource.class);
        try (var connection = dataSource.getConnection();
                var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT 1")) {
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
        assertTrue(context.containsBean("entityManagerFactory"), "JPA must initialize at startup");
    }
}