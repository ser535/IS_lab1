package ru.itmo.khlopensky;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import ru.itmo.khlopensky.entity.Coordinates;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class RouteApplicationTest {
    static final EmbeddedPostgres postgres = startPostgres();

    static EmbeddedPostgres startPostgres() {
        try { return EmbeddedPostgres.builder().start(); }
        catch (IOException exception) { throw new ExceptionInInitializerError(exception); }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> "");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transaction;
    @PersistenceContext EntityManager em;

    @BeforeEach
    void clear() {
        jdbc.execute("TRUNCATE lab1_route, lab1_coordinates, lab1_location RESTART IDENTITY CASCADE");
    }

    JsonNode create(String path, Object body) throws Exception {
        return mapper.readTree(mvc.perform(post("/api/" + path).with(csrf())
                .contentType("application/json").content(mapper.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    Map<String, Object> routeBody(String name, Long distance, int rating) throws Exception {
        int coordinates = create("coordinates", Map.of("x", 509, "y", -896)).get("id").asInt();
        int from = create("locations", Map.of("name", "Начало", "x", 1, "y", 2, "z", 3)).get("id").asInt();
        int to = create("locations", Map.of("name", "Конец", "x", 4, "y", 5, "z", 6)).get("id").asInt();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name); body.put("distance", distance); body.put("rating", rating);
        body.put("coordinatesId", coordinates); body.put("fromId", from); body.put("toId", to);
        return body;
    }

    @Test
    void createsReadsUpdatesAndDeletes() throws Exception {
        var body = routeBody("Первый", 100L, 5);
        JsonNode created = create("routes", body);
        int id = created.get("id").asInt();
        assertTrue(id > 0);
        assertEquals(java.time.LocalDate.now().toString(), created.get("creationDate").asText());
        mvc.perform(get("/api/routes/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.from.name").value("Начало"))
                .andExpect(jsonPath("$.coordinates.y").value("-896"));
        body.put("name", "Обновлённый"); body.put("version", created.get("version").asLong());
        JsonNode updated = mapper.readTree(mvc.perform(put("/api/routes/" + id).with(csrf())
                .contentType("application/json").content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(delete("/api/routes/" + id).param("version", updated.get("version").asText()).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/routes/" + id)).andExpect(status().isNotFound());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM lab1_coordinates", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM lab1_location", Integer.class));
    }

    @Test
    void acceptsNullDistanceAndDestinationAndEmptyLocationName() throws Exception {
        var body = routeBody("Без конца", null, 1); body.put("toId", null);
        create("routes", body);
        create("locations", Map.of("name", "", "x", 0, "y", 0, "z", 0));
        mvc.perform(get("/api/routes")).andExpect(jsonPath("$.items[0].to").isEmpty())
                .andExpect(jsonPath("$.items[0].distance").isEmpty());
    }

    @Test
    void validatesAllFieldBoundaries() throws Exception {
        for (var body : new Object[]{Map.of("x", 510, "y", 0), Map.of("x", 0, "y", -897), Map.of("y", 0)}) {
            mvc.perform(post("/api/coordinates").with(csrf()).contentType("application/json")
                    .content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/locations").with(csrf()).contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("x", 1, "y", 2, "z", 3, "name", "x".repeat(346)))))
                .andExpect(status().isBadRequest());
        var valid = routeBody("Имя", 2L, 1);
        for (var invalid : Map.of("name", " ", "rating", 0, "distance", 1).entrySet()) {
            var body = new LinkedHashMap<>(valid); body.put(invalid.getKey(), invalid.getValue());
            mvc.perform(post("/api/routes").with(csrf()).contentType("application/json")
                    .content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/coordinates").with(csrf()).contentType("application/json")
                .content("{\"x\":1.5,\"y\":0}")).andExpect(status().isBadRequest());
    }

    @Test
    void supportsFullLongRangeWithoutRounding() throws Exception {
        JsonNode coordinates = create("coordinates", Map.of("x", Long.toString(Long.MIN_VALUE), "y", Long.toString(Long.MAX_VALUE)));
        assertEquals(Long.toString(Long.MIN_VALUE), coordinates.get("x").asText());
        var body = routeBody("Дальний", Long.MAX_VALUE, 1);
        assertEquals(Long.toString(Long.MAX_VALUE), create("routes", body).get("distance").asText());
    }

    @Test
    void filtersExactlySortsStringsAndPaginates() throws Exception {
        create("routes", routeBody("Маршрут", 10L, 1));
        create("routes", routeBody("Маршрут 2", 20L, 2));
        mvc.perform(get("/api/routes").param("name", "Маршрут")).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/routes").param("name", "Марш")).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/routes").param("fromName", "Начало").param("toName", "Конец"))
                .andExpect(jsonPath("$.total").value(2));
        mvc.perform(get("/api/routes").param("sort", "name").param("direction", "desc").param("size", "1"))
                .andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].name").value("Маршрут 2"));
        mvc.perform(get("/api/routes").param("page", "1").param("size", "1")).andExpect(jsonPath("$.page").value(1));
        mvc.perform(get("/api/routes").param("sort", "unknown")).andExpect(status().isBadRequest());
    }

    @Test
    void performsAllFiveSpecialOperations() throws Exception {
        var body = routeBody("Короткий", 10L, 3);
        int shortest = create("routes", body).get("id").asInt();
        body.put("name", "Длинный"); body.put("distance", 50L); body.put("rating", 7);
        int longest = create("routes", body).get("id").asInt();
        body.put("name", "Неизвестный"); body.put("distance", null); create("routes", body);
        mvc.perform(get("/api/routes/operations/count").param("rating", "7")).andExpect(content().string("2"));
        mvc.perform(get("/api/routes/operations/count").param("rating", "3").param("greater", "true"))
                .andExpect(content().string("2"));
        mvc.perform(get("/api/routes/operations/above").param("rating", "3")).andExpect(jsonPath("$.length()").value(2));
        for (boolean isLongest : new boolean[]{false, true}) {
            mvc.perform(get("/api/routes/operations/extreme").param("from", body.get("fromId").toString())
                    .param("to", body.get("toId").toString()).param("longest", Boolean.toString(isLongest)))
                    .andExpect(jsonPath("$.id").value(isLongest ? longest : shortest));
        }
        mvc.perform(get("/api/routes/operations/between").param("from", body.get("fromId").toString())
                .param("to", body.get("toId").toString()).param("sort", "name"))
                .andExpect(jsonPath("$.length()").value(3)).andExpect(jsonPath("$[0].name").value("Длинный"));
        mvc.perform(get("/api/routes/operations/extreme").param("from", body.get("toId").toString())
                .param("to", body.get("fromId").toString())).andExpect(status().isNotFound());
    }

    @Test
    void cascadesLocationAndCoordinateDeletion() throws Exception {
        var body = routeBody("Первый", 10L, 1); create("routes", body); create("routes", body);
        mvc.perform(delete("/api/locations/" + body.get("fromId")).with(csrf())).andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM lab1_route", Integer.class));
        body = routeBody("Второй", 10L, 1); create("routes", body);
        mvc.perform(delete("/api/coordinates/" + body.get("coordinatesId")).with(csrf())).andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM lab1_route", Integer.class));
    }

    @Test
    void preservesSharedReferencesWhenDeletingOneRoute() throws Exception {
        var body = routeBody("Общий", 10L, 1);
        JsonNode first = create("routes", body); JsonNode second = create("routes", body);
        mvc.perform(delete("/api/routes/" + first.get("id").asInt()).with(csrf()).param("version", first.get("version").asText()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/routes/" + second.get("id").asInt())).andExpect(status().isOk());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM lab1_coordinates", Integer.class));
    }

    @Test
    void preventsLostUpdatesAndShowsChangesToAnotherUser() throws Exception {
        var body = routeBody("Оригинал", 10L, 1); JsonNode route = create("routes", body);
        body.put("version", route.get("version").asLong()); body.put("name", "Изменён");
        String url = "/api/routes/" + route.get("id").asInt();
        mvc.perform(put(url).with(user("first")).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        mvc.perform(get(url).with(user("second"))).andExpect(jsonPath("$.name").value("Изменён"));
        mvc.perform(put(url).with(user("second")).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
        mvc.perform(delete(url).with(csrf()).param("version", route.get("version").asText())).andExpect(status().isConflict());
    }

    @Test
    void databaseAndOrmEnforceConstraints() {
        assertThrows(Exception.class, () -> jdbc.update("INSERT INTO lab1_coordinates(x,y) VALUES (510,0)"));
        assertThrows(Exception.class, () -> jdbc.update("INSERT INTO lab1_coordinates(x,y) VALUES (0,-897)"));
        assertThrows(Exception.class, () -> jdbc.update("INSERT INTO lab1_location(x,y,z,name) VALUES ('NaN',0,0,'x')"));
        assertThrows(ConstraintViolationException.class, () -> transaction.executeWithoutResult(status -> {
            Coordinates coordinates = new Coordinates(); coordinates.setX(510); coordinates.setY(0L);
            em.persist(coordinates); em.flush();
        }));
    }

    @Test
    void requiresAuthenticationAndCsrf() throws Exception {
        mvc.perform(get("/api/routes").with(anonymous())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/coordinates").contentType("application/json").content("{\"x\":0,\"y\":0}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/session")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }
}
