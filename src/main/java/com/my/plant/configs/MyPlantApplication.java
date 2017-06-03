package com.my.plant.configs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@EnableAutoConfiguration
//@SpringBootApplication
public class MyPlantApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyPlantApplication.class, args);
	}
}
