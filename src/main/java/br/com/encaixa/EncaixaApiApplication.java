package br.com.encaixa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EncaixaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EncaixaApiApplication.class, args);
    }
}
