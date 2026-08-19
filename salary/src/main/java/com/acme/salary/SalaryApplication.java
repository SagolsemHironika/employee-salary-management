package com.acme.salary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SalaryApplication {

	public static void main(String[] args) {
		SpringApplication.run(SalaryApplication.class, args);
	}

}
