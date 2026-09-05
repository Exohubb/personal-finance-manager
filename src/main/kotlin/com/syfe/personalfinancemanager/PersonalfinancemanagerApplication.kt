package com.syfe.personalfinancemanager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Entry point for the Personal Finance Manager API.
 *
 * `@SpringBootApplication` combines `@Configuration`,
 * `@EnableAutoConfiguration`, and `@ComponentScan` - it lets Spring Boot
 * auto-configure sensible defaults for whatever is on the classpath (JPA,
 * H2, Spring Security, etc.) and automatically discover every
 * `@Service`/`@RestController`/`@Repository` in this package and below.
 */
@SpringBootApplication
class PersonalfinancemanagerApplication

fun main(args: Array<String>) {
	runApplication<PersonalfinancemanagerApplication>(*args)
}
