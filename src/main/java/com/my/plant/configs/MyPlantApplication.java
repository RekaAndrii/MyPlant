package com.my.plant.configs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@Configuration
@ComponentScan
@EnableAutoConfiguration
//@SpringBootApplication
public class MyPlantApplication {

	@PostConstruct
	void started() {
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kiev"));
	}

	public static void main(String[] args) {
		SpringApplication.run(MyPlantApplication.class, args);
	}
}
