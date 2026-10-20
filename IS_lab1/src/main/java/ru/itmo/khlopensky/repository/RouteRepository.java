package ru.itmo.khlopensky.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.khlopensky.entity.Route;
import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Integer>, JpaSpecificationExecutor<Route> {
    long countByRating(int rating);

    long countByRatingGreaterThan(int rating);

    List<Route> findByRatingGreaterThanOrderByRatingAscIdAsc(int rating);

    List<Route> findByFrom_IdAndTo_Id(int from, int to, Sort sort);

    boolean existsByCoordinates_Id(int id);

    boolean existsByFrom_IdOrTo_Id(int from, int to);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Route r WHERE r.coordinates.id = :id")
    void deleteByCoordinatesId(@Param("id") int id);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Route r WHERE r.from.id = :id OR r.to.id = :id")
    void deleteByLocationId(@Param("id") int id);
}
