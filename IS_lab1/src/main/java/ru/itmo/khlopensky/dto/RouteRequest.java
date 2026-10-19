package ru.itmo.khlopensky.dto;

import jakarta.validation.constraints.*;

public record RouteRequest(
        @NotBlank(message = "Название не должно быть пустым") String name,
        @NotNull(message = "Выберите координаты") @Positive Integer coordinatesId,
        @NotNull(message = "Выберите начальную локацию") @Positive Integer fromId,
        @Positive Integer toId,
        @Min(value = 2, message = "Расстояние должно быть больше 1") Long distance,
        @NotNull(message = "Укажите рейтинг") @Positive(message = "Рейтинг должен быть больше 0") Integer rating,
        @PositiveOrZero Long version) {
}
