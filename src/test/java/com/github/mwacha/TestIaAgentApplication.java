package com.github.mwacha;

import org.springframework.boot.SpringApplication;

public class TestIaAgentApplication {

	public static void main(String[] args) {
		SpringApplication.from(Application::main).with(TestcontainersConfiguration.class).run(args);
	}

}
