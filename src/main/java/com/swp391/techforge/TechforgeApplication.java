package com.swp391.techforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@EnableScheduling
public class TechforgeApplication {

	public static void main(String[] args) {
		SpringApplication.run(TechforgeApplication.class, args);
	}

	@Bean
	public CommandLineRunner runAlterTable(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				jdbcTemplate.execute("ALTER TABLE orders MODIFY COLUMN status ENUM('PENDING','CONFIRMED','SHIPPING','DELIVERED','COMPLETED','COMPLAINT','CANCEL_REQUESTED','CANCELLED') DEFAULT 'PENDING'");
				System.out.println("SQL: ALTER TABLE orders successful.");
			} catch (Exception e) {
				System.out.println("SQL: ALTER TABLE orders failed: " + e.getMessage());
			}
		};
	}
}
