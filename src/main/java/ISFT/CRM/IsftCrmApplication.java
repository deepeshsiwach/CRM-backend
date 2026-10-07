package ISFT.CRM;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class IsftCrmApplication {

	public static void main(String[] args) {
		SpringApplication.run(IsftCrmApplication.class, args);
	}

}
