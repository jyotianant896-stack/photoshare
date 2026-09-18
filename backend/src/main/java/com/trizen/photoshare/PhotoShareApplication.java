package com.trizen.photoshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PhotoShareApplication {
    public static void main(String[] args) {
        SpringApplication.run(PhotoShareApplication.class, args);
    }
}
