package com.example.backend.game;

public class NotEnoughCookiesException extends RuntimeException {

    public NotEnoughCookiesException(long cost, double available) {
        super("Upgrade costs " + cost + " cookies, you have " + (long) available);
    }
}
