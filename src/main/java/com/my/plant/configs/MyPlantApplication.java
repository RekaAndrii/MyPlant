package com.my.plant.configs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class MyPlantApplication {

	@PostConstruct
	void started() {
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kiev"));
	}

	public static void main(String[] args) {
		SpringApplication.run(MyPlantApplication.class, args);
	}

	@Bean
	public OpenAPI newsApi() {
		return new OpenAPI().info(new Info()
				.title("My plant application to grow")
				.description("It is app that helps you grow")
				.version("2.0"));
	}
}
