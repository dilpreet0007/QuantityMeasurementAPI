package com.app.quantity_measurement_app;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.app.quantity_measurement_app.repository")
@OpenAPIDefinition(
    info = @Info(
        title = "Quantity Measurement API",
        version = "1.0.0",
        description = "REST API for quantity measurements with support for multiple unit types"
    )
)
public class QuantityMeasurementApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuantityMeasurementApplication.class, args);
        System.out.println("Quantity Measurement Application is running...");
    }
}

