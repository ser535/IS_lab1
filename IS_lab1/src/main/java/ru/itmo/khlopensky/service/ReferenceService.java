package ru.itmo.khlopensky.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.khlopensky.dto.*;
import ru.itmo.khlopensky.entity.*;
import ru.itmo.khlopensky.repository.ReferenceRepository;
import java.util.List;

@Service
@Transactional
public class ReferenceService {
    private final ReferenceRepository repository;

    public ReferenceService(ReferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Coordinates> coordinates() { return repository.coordinates(); }

    @Transactional(readOnly = true)
    public List<Location> locations() { return repository.locations(); }

    public Coordinates coordinates(int id) {
        Coordinates result = repository.find(Coordinates.class, id);
        if (result == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Координаты не найдены");
        return result;
    }

    public Location location(int id) {
        Location result = repository.find(Location.class, id);
        if (result == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Локация не найдена");
        return result;
    }

    public Coordinates create(CoordinatesRequest request) {
        Coordinates result = new Coordinates();
        result.setX(request.x());
        result.setY(request.y());
        repository.save(result);
        return result;
    }

    public Location create(LocationRequest request) {
        Location result = new Location();
        result.setX(request.x());
        result.setY(request.y());
        result.setZ(request.z());
        result.setName(request.name());
        repository.save(result);
        return result;
    }

    public void deleteCoordinates(int id) { repository.deleteCoordinates(coordinates(id)); }
    public void deleteLocation(int id) { repository.deleteLocation(location(id)); }
}
