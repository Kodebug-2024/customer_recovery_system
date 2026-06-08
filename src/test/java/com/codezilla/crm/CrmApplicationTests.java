package com.codezilla.crm;

import com.codezilla.crm.testsupport.RedisMockConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(RedisMockConfig.class)
class CrmApplicationTests {

    @Test
    void contextLoads() {
    }
}
