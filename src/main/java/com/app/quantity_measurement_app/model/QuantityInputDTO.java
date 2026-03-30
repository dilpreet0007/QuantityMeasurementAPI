package com.app.quantity_measurement_app.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(example = """
        {
          "thisQuantityDTO":   { "value": 1.0,  "unit": "FEET",   "measurementType": "LengthUnit" },
          "thatQuantityDTO":   { "value": 12.0, "unit": "INCHES", "measurementType": "LengthUnit" },
          "targetQuantityDTO": { "value": 0.0,  "unit": "INCHES", "measurementType": "LengthUnit" }
        }
        """)
public class QuantityInputDTO {

    @Valid
    @NotNull(message = "First quantity cannot be null")
    private QuantityDTO thisQuantityDTO;

    @Valid
    @NotNull(message = "Second quantity cannot be null")
    private QuantityDTO thatQuantityDTO;

    // Optional field for addition and subtraction operations
    @Valid
    @Schema(nullable = true)
    private QuantityDTO targetQuantityDTO;

    // --- Getters and Setters ---
    public QuantityDTO getThisQuantityDTO() {
        return thisQuantityDTO;
    }

    public void setThisQuantityDTO(QuantityDTO thisQuantityDTO) {
        this.thisQuantityDTO = thisQuantityDTO;
    }

    public QuantityDTO getThatQuantityDTO() {
        return thatQuantityDTO;
    }

    public void setThatQuantityDTO(QuantityDTO thatQuantityDTO) {
        this.thatQuantityDTO = thatQuantityDTO;
    }

    public QuantityDTO getTargetQuantityDTO() {
        return targetQuantityDTO;
    }

    public void setTargetQuantityDTO(QuantityDTO targetQuantityDTO) {
        this.targetQuantityDTO = targetQuantityDTO;
    }

    // --- Optional: toString for debugging ---
    @Override
    public String toString() {
        return "QuantityInputDTO{" +
                "thisQuantityDTO=" + thisQuantityDTO +
                ", thatQuantityDTO=" + thatQuantityDTO +
                ", targetQuantityDTO=" + targetQuantityDTO +
                '}';
    }
}
