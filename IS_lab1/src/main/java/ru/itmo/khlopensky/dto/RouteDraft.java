package ru.itmo.khlopensky.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RouteDraft(
        @Positive Integer id,
        boolean recreateDeleted,
        @NotNull @Valid RouteRequest route,
        @Valid CoordinatesRequest coordinates,
        @Valid LocationRequest from,
        @Valid LocationRequest to) {
}
