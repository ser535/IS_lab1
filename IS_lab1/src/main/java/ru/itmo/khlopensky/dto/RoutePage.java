package ru.itmo.khlopensky.dto;

import ru.itmo.khlopensky.entity.Route;
import java.util.List;

public record RoutePage(List<Route> items, long total, int page, int size) {
}
