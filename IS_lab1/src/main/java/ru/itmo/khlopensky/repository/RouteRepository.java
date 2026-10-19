package ru.itmo.khlopensky.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import ru.itmo.khlopensky.dto.RoutePage;
import ru.itmo.khlopensky.entity.Route;
import java.util.*;

@Repository
public class RouteRepository {
    @PersistenceContext
    private EntityManager em;

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "r.id", "name", "r.name", "creationDate", "r.creationDate",
            "fromName", "f.name", "toName", "t.name", "distance", "r.distance", "rating", "r.rating");
    private static final String SELECT = " FROM Route r JOIN r.from f LEFT JOIN r.to t";

    public Route find(int id) {
        return em.find(Route.class, id);
    }

    public void save(Route route) {
        em.persist(route);
    }

    public void delete(Route route) {
        em.remove(route);
    }

    public RoutePage page(int page, int size, String name, String fromName, String toName, String sort, String direction) {
        Map<String, Object> parameters = new HashMap<>();
        String where = " WHERE 1=1";
        if (name != null) { where += " AND r.name = :name"; parameters.put("name", name); }
        if (fromName != null) { where += " AND f.name = :fromName"; parameters.put("fromName", fromName); }
        if (toName != null) { where += " AND t.name = :toName"; parameters.put("toName", toName); }
        long total = bind(em.createQuery("SELECT COUNT(r)" + SELECT + where, Long.class), parameters).getSingleResult();
        int actualPage = (int) Math.min(page, Math.max(0, (total - 1) / size));
        List<Route> items = bind(em.createQuery("SELECT r" + SELECT + where + order(sort, direction), Route.class), parameters)
                .setFirstResult(actualPage * size).setMaxResults(size).getResultList();
        return new RoutePage(items, total, actualPage, size);
    }

    public long countRating(int rating, boolean greater) {
        return em.createQuery("SELECT COUNT(r) FROM Route r WHERE r.rating " + (greater ? ">" : "=") + " :rating", Long.class)
                .setParameter("rating", rating).getSingleResult();
    }

    public List<Route> aboveRating(int rating) {
        return em.createQuery("SELECT r FROM Route r WHERE r.rating > :rating ORDER BY r.rating, r.id", Route.class)
                .setParameter("rating", rating).getResultList();
    }

    public List<Route> between(int from, int to, String sort, String direction) {
        return em.createQuery("SELECT r" + SELECT + " WHERE f.id = :from AND t.id = :to" + order(sort, direction), Route.class)
                .setParameter("from", from).setParameter("to", to).getResultList();
    }

    private String order(String sort, String direction) {
        String field = SORT_FIELDS.get(sort);
        if (field == null || !(direction.equals("asc") || direction.equals("desc"))) {
            throw new IllegalArgumentException("Недопустимые параметры сортировки");
        }
        return " ORDER BY " + field + " " + direction + (sort.equals("id") ? "" : ", r.id ASC");
    }

    private <T> TypedQuery<T> bind(TypedQuery<T> query, Map<String, Object> parameters) {
        parameters.forEach(query::setParameter);
        return query;
    }
}
