package ru.itmo.khlopensky.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.khlopensky.dto.*;
import ru.itmo.khlopensky.entity.*;
import ru.itmo.khlopensky.repository.*;
import org.springframework.data.domain.Sort;
import java.util.List;

@Service
@Transactional
public class ReferenceService {
    private final CoordinatesRepository coordinatesRepository;
    private final LocationRepository locationRepository;
    private final RouteRepository routeRepository;

    public ReferenceService(CoordinatesRepository coordinatesRepository, LocationRepository locationRepository,
                            RouteRepository routeRepository) {
        this.coordinatesRepository = coordinatesRepository;
        this.locationRepository = locationRepository;
        this.routeRepository = routeRepository;
    }

    @Transactional(readOnly = true)
    public List<Coordinates> coordinates() {
        return coordinatesRepository.findAll(Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public List<Location> locations() {
        return locationRepository.findAll(Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public Coordinates coordinates(int id) {
        return coordinatesRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Координаты не найдены"));
    }

    @Transactional(readOnly = true)
    public Location location(int id) {
        return locationRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Локация не найдена"));
    }

    public Coordinates create(CoordinatesRequest request) {
        Coordinates result = new Coordinates();
        result.setX(request.x());
        result.setY(request.y());
        return coordinatesRepository.save(result);
    }

    public Location create(LocationRequest request) {
        Location result = new Location();
        result.setX(request.x());
        result.setY(request.y());
        result.setZ(request.z());
        result.setName(request.name());
        return locationRepository.save(result);
    }

    public void deleteCoordinates(int id) {
        Coordinates coordinates = coordinates(id);
        routeRepository.deleteByCoordinatesId(id);
        coordinatesRepository.delete(coordinates);
    }

    public void deleteLocation(int id) {
        Location location = location(id);
        routeRepository.deleteByLocationId(id);
        locationRepository.delete(location);
    }
}
