package ru.itmo.khlopensky.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.itmo.khlopensky.dto.*;
import ru.itmo.khlopensky.entity.Route;
import ru.itmo.khlopensky.service.RouteService;
import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {
    private final RouteService service;

    public RouteController(RouteService service) { this.service = service; }

    @GetMapping
    public RoutePage list(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String name,
                          @RequestParam(required = false) String fromName,
                          @RequestParam(required = false) String toName,
                          @RequestParam(defaultValue = "id") String sort,
                          @RequestParam(defaultValue = "asc") String direction) {
        return service.page(page, size, name, fromName, toName, sort, direction);
    }

    @GetMapping("/{id}")
    public Route get(@PathVariable int id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Route create(@Valid @RequestBody RouteRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    public Route update(@PathVariable int id, @Valid @RequestBody RouteRequest request) { return service.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id, @RequestParam long version) { service.delete(id, version); }

    @GetMapping("/operations/count")
    public long count(@RequestParam int rating, @RequestParam(defaultValue = "false") boolean greater) {
        return service.countRating(rating, greater);
    }

    @GetMapping("/operations/above")
    public List<Route> above(@RequestParam int rating) { return service.aboveRating(rating); }

    @GetMapping("/operations/between")
    public List<Route> between(@RequestParam int from, @RequestParam int to,
                               @RequestParam(defaultValue = "id") String sort,
                               @RequestParam(defaultValue = "asc") String direction) {
        return service.between(from, to, sort, direction);
    }

    @GetMapping("/operations/extreme")
    public Route extreme(@RequestParam int from, @RequestParam int to,
                         @RequestParam(defaultValue = "false") boolean longest) {
        return service.extreme(from, to, longest);
    }
}
