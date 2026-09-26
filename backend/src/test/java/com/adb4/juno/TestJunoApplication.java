package com.adb4.juno;

import org.springframework.boot.SpringApplication;

public class TestJunoApplication {

	public static void main(String[] args) {
		SpringApplication.from(JunoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
