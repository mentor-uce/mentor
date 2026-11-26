package com.mentor;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Import({PostgreSqlTestConfiguration.class, MongoDbTestConfiguration.class, RedisTestConfiguration.class, JwtTestConfiguration.class})
class MongoConnectivityTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void configuredMongoTemplateCanPingMongoDb() {
        var mongo = context.getBean(MongoTemplate.class);
        var result = mongo.executeCommand(new Document("ping", 1));
        assertEquals(1.0, ((Number) result.get("ok")).doubleValue());
    }
}