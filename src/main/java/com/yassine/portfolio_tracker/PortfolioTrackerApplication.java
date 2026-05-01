package com.yassine.portfolio_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PortfolioTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PortfolioTrackerApplication.class, args);
	}

}
