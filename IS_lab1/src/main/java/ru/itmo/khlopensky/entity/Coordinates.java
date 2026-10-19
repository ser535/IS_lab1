package ru.itmo.khlopensky.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "lab1_coordinates")
public class Coordinates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Positive
    private Integer id;

    @Max(value = 509, message = "X должен быть не больше 509")
    @Column(nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private long x;

    @NotNull
    @Min(value = -896, message = "Y должен быть больше -897")
    @Column(nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long y;

    public Integer getId() {
        return id;
    }

    public long getX() {
        return x;
    }

    public void setX(long x) {
        this.x = x;
    }

    public Long getY() {
        return y;
    }

    public void setY(Long y) {
        this.y = y;
    }
}
