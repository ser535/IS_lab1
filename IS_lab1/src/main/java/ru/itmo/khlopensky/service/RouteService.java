package ru.itmo.khlopensky.service;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
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
    private final CoordinatesRepository coordinatesRepository;
    private final LocationRepository locationRepository;
    private final ReferenceService referenceService;

    public RouteService(RouteRepository repository, CoordinatesRepository coordinatesRepository,
                        LocationRepository locationRepository, ReferenceService referenceService) {
        this.repository = repository;
        this.coordinatesRepository = coordinatesRepository;
        this.locationRepository = locationRepository;
        this.referenceService = referenceService;
    }

    @Transactional(readOnly = true)
    public RoutePage page(int page, int size, String name, String fromName, String toName, String sort, String direction) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Страница должна быть от 0, размер — от 1 до 100");
        Sort sorting = sort(sort, direction);
        Specification<Route> filter = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null) predicates.add(builder.equal(root.get("name"), name));
            if (fromName != null) predicates.add(builder.equal(root.get("from").get("name"), fromName));
            if (toName != null) predicates.add(builder.equal(root.join("to", JoinType.LEFT).get("name"), toName));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        long total = repository.count(filter);
        int actualPage = (int) Math.min(page, Math.max(0, (total - 1) / size));
        List<Route> items = repository.findAll(filter, PageRequest.of(actualPage, size, sorting)).getContent();
        return new RoutePage(items, total, actualPage, size);
    }

    @Transactional(readOnly = true)
    public Route get(int id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Маршрут не найден или уже удалён"));
    }

    public Route create(RouteRequest request) {
        Route route = new Route();
        fill(route, request);
        return repository.save(route);
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
        repository.flush();
        if (!repository.existsByCoordinates_Id(coordinates.getId())) {
            coordinatesRepository.delete(coordinates);
        }
        if (!repository.existsByFrom_IdOrTo_Id(from.getId(), from.getId())) {
            locationRepository.delete(from);
        }
        if (to != null && !to.getId().equals(from.getId())
                && !repository.existsByFrom_IdOrTo_Id(to.getId(), to.getId())) {
            locationRepository.delete(to);
        }
    }

    @Transactional(readOnly = true)
    public long countRating(int rating, boolean greater) { return greater ? repository.countByRatingGreaterThan(rating) : repository.countByRating(rating); }

    @Transactional(readOnly = true)
    public List<Route> aboveRating(int rating) { return repository.findByRatingGreaterThanOrderByRatingAscIdAsc(rating); }

    @Transactional(readOnly = true)
    public List<Route> between(int from, int to, String sort, String direction) {
        referenceService.location(from);
        referenceService.location(to);
        return repository.findByFrom_IdAndTo_Id(from, to, sort(sort, direction));
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

    private Sort sort(String field, String direction) {
        String property = switch (field) {
            case "id", "name", "creationDate", "distance", "rating" -> field;
            case "fromName" -> "from.name";
            case "toName" -> "to.name";
            default -> throw new IllegalArgumentException("Недопустимые параметры сортировки");
        };
        if (!"asc".equals(direction) && !"desc".equals(direction)) {
            throw new IllegalArgumentException("Недопустимые параметры сортировки");
        }
        Sort result = Sort.by(Sort.Direction.fromString(direction), property);
        return field.equals("id") ? result : result.and(Sort.by("id"));
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
