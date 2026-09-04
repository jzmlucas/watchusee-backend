package br.com.watchusee.watchusee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class WatchuseeApplication {

	public static void main(String[] args) {
		SpringApplication.run(WatchuseeApplication.class, args);
	}

}
