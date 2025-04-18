package cn.huo.ohmqttserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OhMqttServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(OhMqttServerApplication.class, args);
	}

}
