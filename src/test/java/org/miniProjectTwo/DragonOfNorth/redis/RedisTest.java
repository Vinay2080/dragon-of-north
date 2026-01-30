package org.miniProjectTwo.DragonOfNorth.redis;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public class RedisTest {

    private final StringRedisTemplate redisTemplate;

    RedisTest(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Test
    void shouldStoreAndReadValueFromRedis() {
        redisTemplate.opsForValue().set("k", "v");
        assertEquals("v", redisTemplate.opsForValue().get("k"));
    }

    @AfterEach
    void cleanup() {
        redisTemplate.delete("k");
    }


}
