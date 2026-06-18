package com.lazybeartoby.btcmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BtcmarketApplication {

	public static void main(String[] args) {
		SpringApplication.run(BtcmarketApplication.class, args);
	}

}
