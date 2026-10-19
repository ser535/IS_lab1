package ru.itmo.khlopensky.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "lab1_route")
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Positive
    private Integer id;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    @NotNull
    @Valid
    @ManyToOne(optional = false)
    @JoinColumn(name = "coordinates_id", nullable = false)
    private Coordinates coordinates;

    @NotNull
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDate creationDate;

    @NotNull
    @Valid
    @ManyToOne(optional = false)
    @JoinColumn(name = "from_id", nullable = false)
    private Location from;

    @Valid
    @ManyToOne
    @JoinColumn(name = "to_id")
    private Location to;

    @Min(2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long distance;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer rating;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    void initializeDate() {
        creationDate = LocalDate.now();
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public Location getFrom() {
        return from;
    }

    public void setFrom(Location from) {
        this.from = from;
    }

    public Location getTo() {
        return to;
    }

    public void setTo(Location to) {
        this.to = to;
    }

    public Long getDistance() {
        return distance;
    }

    public void setDistance(Long distance) {
        this.distance = distance;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public long getVersion() {
        return version;
    }
}
