package ru.itmo.khlopensky.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.itmo.khlopensky.dto.*;
import ru.itmo.khlopensky.entity.*;
import ru.itmo.khlopensky.service.ReferenceService;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReferenceController {
    private final ReferenceService service;

    public ReferenceController(ReferenceService service) { this.service = service; }

    @GetMapping("/coordinates")
    public List<Coordinates> coordinates() { return service.coordinates(); }

    @GetMapping("/locations")
    public List<Location> locations() { return service.locations(); }

    @PostMapping("/coordinates")
    @ResponseStatus(HttpStatus.CREATED)
    public Coordinates createCoordinates(@Valid @RequestBody CoordinatesRequest request) { return service.create(request); }

    @PostMapping("/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public Location createLocation(@Valid @RequestBody LocationRequest request) { return service.create(request); }

    @DeleteMapping("/coordinates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCoordinates(@PathVariable int id) { service.deleteCoordinates(id); }

    @DeleteMapping("/locations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLocation(@PathVariable int id) { service.deleteLocation(id); }
}
