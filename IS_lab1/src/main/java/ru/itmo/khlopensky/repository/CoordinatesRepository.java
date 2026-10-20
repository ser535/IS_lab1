package ru.itmo.khlopensky.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.khlopensky.entity.Coordinates;

public interface CoordinatesRepository extends JpaRepository<Coordinates, Integer> {
}
