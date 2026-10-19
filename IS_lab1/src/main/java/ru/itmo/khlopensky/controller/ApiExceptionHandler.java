package ru.itmo.khlopensky.controller;

import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage()).collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException exception) {
        return error(exception.getStatusCode(), exception.getReason());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> argument(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "Некорректное значение параметра: " + exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> unreadable() {
        return error(HttpStatus.BAD_REQUEST, "Проверьте типы и диапазоны чисел. Целые числа не должны содержать дробную часть; расстояние и координаты должны помещаться в соответствующий тип.");
    }

    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<?> conflict() {
        return error(HttpStatus.CONFLICT, "Объект уже изменён другим пользователем. Откройте его заново.");
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ConstraintViolationException.class, TransactionSystemException.class})
    public ResponseEntity<?> integrity() {
        return error(HttpStatus.CONFLICT, "Изменение не сохранено: проверьте ограничения полей и существование выбранных объектов. Обновите данные.");
    }

    private ResponseEntity<?> error(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message == null ? "Ошибка запроса" : message));
    }
}
