package ru.itmo.khlopensky.dto;

import jakarta.validation.constraints.*;

public record CoordinatesRequest(
        @NotNull(message = "Укажите X") @Max(value = 509, message = "X должен быть не больше 509") Long x,
        @NotNull(message = "Укажите Y") @Min(value = -896, message = "Y должен быть больше -897") Long y) {
}
