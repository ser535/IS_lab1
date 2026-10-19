package ru.itmo.khlopensky.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "lab1_location")
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Positive
    private Integer id;

    @NotNull
    @Column(nullable = false)
    private Float x;

    @NotNull
    @Column(nullable = false)
    private Double y;

    @NotNull
    @Column(nullable = false)
    private Double z;

    @NotNull
    @Size(max = 345)
    @Column(nullable = false, length = 345)
    private String name;

    @AssertTrue(message = "Координаты локации должны быть конечными числами")
    @JsonIgnore
    public boolean isFinite() {
        return (x == null || Float.isFinite(x)) && (y == null || Double.isFinite(y))
                && (z == null || Double.isFinite(z));
    }

    public Integer getId() {
        return id;
    }

    public Float getX() {
        return x;
    }

    public void setX(Float x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getZ() {
        return z;
    }

    public void setZ(Double z) {
        this.z = z;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
