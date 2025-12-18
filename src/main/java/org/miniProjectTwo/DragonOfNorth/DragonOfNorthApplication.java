package org.miniProjectTwo.DragonOfNorth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class DragonOfNorthApplication {

    static void main(String[] args) {
        SpringApplication.run(DragonOfNorthApplication.class, args);
    }

}
