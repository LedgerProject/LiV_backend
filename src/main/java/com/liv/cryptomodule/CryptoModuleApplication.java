package com.liv.cryptomodule;

import com.liv.cryptomodule.property.FileStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.sql.SQLException;

@SpringBootApplication
@EnableConfigurationProperties({
		FileStorageProperties.class
})
public class CryptoModuleApplication {

	public static void main(String[] args) throws IOException, SQLException {
		SpringApplication.run(CryptoModuleApplication.class, args);
	}

	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
