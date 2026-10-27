package com.flightwatcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class FlightwatcherApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlightwatcherApplication.class, args);
	}

}
