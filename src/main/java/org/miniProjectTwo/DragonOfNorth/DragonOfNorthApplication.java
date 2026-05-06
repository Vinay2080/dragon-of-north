package org.miniProjectTwo.DragonOfNorth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableAsync
@SpringBootApplication
@EnableScheduling
public class DragonOfNorthApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(DragonOfNorthApplication.class, args);
    }
    //todo cleanup code add modularity for service classes.
    //todo find out why we are using ip address as base link for frontend.
    //todo update env before pushing to prod.
}