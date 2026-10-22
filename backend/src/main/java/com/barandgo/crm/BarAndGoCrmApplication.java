package com.barandgo.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@SpringBootApplication
@Modulithic(systemName = "Bar&Go CRM", sharedModules = "shared")
public class BarAndGoCrmApplication {

	public static void main(String[] args) {
		SpringApplication.run(BarAndGoCrmApplication.class, args);
	}

}
