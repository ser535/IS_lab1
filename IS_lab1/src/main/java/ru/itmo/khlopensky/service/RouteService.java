package ru.itmo.khlopensky.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.khlopensky.dto.*;
import ru.itmo.khlopensky.entity.*;
import ru.itmo.khlopensky.repository.*;
import java.util.*;

@Service
@Transactional
public class RouteService {
    private final RouteRepository repository;
    private final ReferenceRepository references;
    private final ReferenceService referenceService;

    public RouteService(RouteRepository repository, ReferenceRepository references, ReferenceService referenceService) {
        this.repository = repository;
        this.references = references;
        this.referenceService = referenceService;
    }

    @Transactional(readOnly = true)
    public RoutePage page(int page, int size, String name, String fromName, String toName, String sort, String direction) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Страница должна быть от 0, размер — от 1 до 100");
        return repository.page(page, size, name, fromName, toName, sort, direction);
    }

    @Transactional(readOnly = true)
    public Route get(int id) {
        Route route = repository.find(id);
        if (route == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Маршрут не найден или уже удалён");
        return route;
    }

    public Route create(RouteRequest request) {
        Route route = new Route();
        fill(route, request);
        repository.save(route);
        return route;
    }

    public Route update(int id, RouteRequest request) {
        Route route = get(id);
        if (request.version() == null || request.version() != route.getVersion()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Маршрут уже изменён другим пользователем. Откройте окно изменения заново.");
        }
        fill(route, request);
        return route;
    }

    public void delete(int id, long version) {
        Route route = get(id);
        if (route.getVersion() != version) throw new ResponseStatusException(HttpStatus.CONFLICT, "Маршрут изменился. Откройте окно удаления заново.");
        Coordinates coordinates = route.getCoordinates();
        Location from = route.getFrom();
        Location to = route.getTo();
        repository.delete(route);
        references.deleteUnusedCoordinates(coordinates);
        references.deleteUnusedLocation(from);
        if (to != null && !to.getId().equals(from.getId())) references.deleteUnusedLocation(to);
    }

    @Transactional(readOnly = true)
    public long countRating(int rating, boolean greater) { return repository.countRating(rating, greater); }

    @Transactional(readOnly = true)
    public List<Route> aboveRating(int rating) { return repository.aboveRating(rating); }

    @Transactional(readOnly = true)
    public List<Route> between(int from, int to, String sort, String direction) {
        referenceService.location(from);
        referenceService.location(to);
        return repository.between(from, to, sort, direction);
    }

    @Transactional(readOnly = true)
    public Route extreme(int from, int to, boolean longest) {
        List<Route> routes = between(from, to, "id", "asc");
        Comparator<Route> comparator = Comparator.comparing(Route::getDistance);
        if (longest) comparator = comparator.reversed();
        return routes.stream().filter(route -> route.getDistance() != null)
                .min(comparator.thenComparing(Route::getId)).orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Нет маршрутов с известным расстоянием между этими локациями"));
    }

    private void fill(Route route, RouteRequest request) {
        route.setName(request.name());
        route.setCoordinates(referenceService.coordinates(request.coordinatesId()));
        route.setFrom(referenceService.location(request.fromId()));
        route.setTo(request.toId() == null ? null : referenceService.location(request.toId()));
        route.setDistance(request.distance());
        route.setRating(request.rating());
    }
}
