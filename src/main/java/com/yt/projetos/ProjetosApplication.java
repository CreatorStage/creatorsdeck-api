package com.yt.projetos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ProjetosApplication {

	public static void main(String[] args) {
		String userHome = System.getProperty("user.home");
		java.io.File dir = new java.io.File(userHome + "/.creatorsdeck");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		SpringApplication.run(ProjetosApplication.class, args);
	}

}
