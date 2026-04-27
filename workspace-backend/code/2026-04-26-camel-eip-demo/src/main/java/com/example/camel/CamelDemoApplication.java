package com.example.camel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot bootstrap for the Camel EIP demo.
 *
 * <p>All Camel routes are auto-discovered from {@code com.example.camel.routes}
 * via Spring component scan + {@code @Component} annotations on each
 * {@code RouteBuilder} subclass.</p>
 *
 * <p><b>Why this is minimal:</b> Camel-Spring-Boot integration handles the
 * {@code CamelContext} lifecycle automatically. No manual {@code main()} bean
 * registration like older versions.</p>
 */
@SpringBootApplication
public class CamelDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CamelDemoApplication.class, args);
    }
}
