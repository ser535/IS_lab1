package ru.itmo.khlopensky.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.khlopensky.entity.Location;

public interface LocationRepository extends JpaRepository<Location, Integer> {
}
