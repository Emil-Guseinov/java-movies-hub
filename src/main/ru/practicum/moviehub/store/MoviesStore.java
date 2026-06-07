package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private int nextId = 1;

    public List<Movie> getAll() {
        return List.copyOf(movies.values());
    }

    public Optional<Movie> getId(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public Movie add(Movie movie) {
        if (movie == null) {
            throw new IllegalArgumentException("Фильм не должен быть null");
        }
        if (movie.getTitle() == null || movie.getTitle().isBlank()) {
            throw new IllegalArgumentException("Название фильма не может быть пустым");
        }
        for (Movie mov : movies.values()) {
            if (mov.getTitle().equalsIgnoreCase(movie.getTitle()) &&
                    mov.getYear() == movie.getYear()) {
                throw new IllegalArgumentException("Фильм с таким названием и годом существует!");
            }
        }

        movie.setId(nextId++);
        movies.put(movie.getId(), movie);
        return movie;
    }

    public boolean exists(int id) {
        return movies.containsKey(id);
    }

    public boolean delete(int id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
        nextId = 1;
    }

}