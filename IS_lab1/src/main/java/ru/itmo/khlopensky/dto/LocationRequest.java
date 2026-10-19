package ru.itmo.khlopensky.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;

public record LocationRequest(
        @NotNull(message = "Укажите X") Float x,
        @NotNull(message = "Укажите Y") Double y,
        @NotNull(message = "Укажите Z") Double z,
        @NotNull(message = "Укажите название") @Size(max = 345, message = "Название: не более 345 символов") String name) {
    @AssertTrue(message = "Координаты локации должны быть конечными числами")
    @JsonIgnore
    public boolean isFinite() {
        return (x == null || Float.isFinite(x)) && (y == null || Double.isFinite(y))
                && (z == null || Double.isFinite(z));
    }
}
