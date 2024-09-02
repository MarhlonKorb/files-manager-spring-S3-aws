package com.upload_files_app.upload_files_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class UploadFilesAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(UploadFilesAppApplication.class, args);
	}

}
