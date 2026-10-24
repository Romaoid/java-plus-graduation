package ru.practicum.user.exceprion;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}