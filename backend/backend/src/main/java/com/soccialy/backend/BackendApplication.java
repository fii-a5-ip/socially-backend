package com.soccialy.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	public CommandLineRunner runSchemaUpdate(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				jdbcTemplate.execute("ALTER TABLE users MODIFY profile_img_url LONGTEXT");
				System.out.println("Successfully altered users table profile_img_url to LONGTEXT");
			} catch (Exception e) {
				System.out.println("Schema update skipped or failed: " + e.getMessage());
			}
		};
	}

}
