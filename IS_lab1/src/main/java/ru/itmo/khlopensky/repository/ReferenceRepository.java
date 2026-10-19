package ru.itmo.khlopensky.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import ru.itmo.khlopensky.entity.Coordinates;
import ru.itmo.khlopensky.entity.Location;
import java.util.List;

@Repository
public class ReferenceRepository {
    @PersistenceContext
    private EntityManager em;

    public List<Coordinates> coordinates() {
        return em.createQuery("SELECT c FROM Coordinates c ORDER BY c.id", Coordinates.class).getResultList();
    }

    public List<Location> locations() {
        return em.createQuery("SELECT l FROM Location l ORDER BY l.id", Location.class).getResultList();
    }

    public <T> T find(Class<T> type, int id) {
        return em.find(type, id);
    }

    public void save(Object object) {
        em.persist(object);
    }

    public void deleteCoordinates(Coordinates coordinates) {
        em.createQuery("DELETE FROM Route r WHERE r.coordinates.id = :id")
                .setParameter("id", coordinates.getId()).executeUpdate();
        em.remove(coordinates);
    }

    public void deleteLocation(Location location) {
        em.createQuery("DELETE FROM Route r WHERE r.from.id = :id OR r.to.id = :id")
                .setParameter("id", location.getId()).executeUpdate();
        em.remove(location);
    }

    public void deleteUnusedCoordinates(Coordinates coordinates) {
        em.flush();
        if (em.createQuery("SELECT COUNT(r) FROM Route r WHERE r.coordinates.id = :id", Long.class)
                .setParameter("id", coordinates.getId()).getSingleResult() == 0) em.remove(coordinates);
    }

    public void deleteUnusedLocation(Location location) {
        em.flush();
        if (em.createQuery("SELECT COUNT(r) FROM Route r WHERE r.from.id = :id OR r.to.id = :id", Long.class)
                .setParameter("id", location.getId()).getSingleResult() == 0) em.remove(location);
    }
}
