package ru.practicum.moviehub.http;


import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if (method.equalsIgnoreCase("GET")) {
                if (path.matches("/movies/\\d+")) {
                    handleGetByIdRequest(ex);
                } else if (path.equals("/movies")) {
                    handleGetRequest(ex);
                } else {
                    sendMethodNotAllowed(ex);
                }
            } else if (method.equalsIgnoreCase("POST")) {
                handlePostRequest(ex);
            } else if (method.equalsIgnoreCase("DELETE")) {

                handleDeleteRequest(ex);
            } else {
                sendMethodNotAllowed(ex);
            }
        } catch (Exception e) {
            sendJson(ex, 500, gson.toJson(new ErrorResponse("Ошибка в работе сервера")));
        }
    }

    private void handleGetRequest(HttpExchange ex) throws IOException {

        List<Movie> movies = store.getAll();
        String json = gson.toJson(movies);
        sendJson(ex, 200, json);


    }

    private void handleGetByIdRequest(HttpExchange ex) throws IOException {

        String path = ex.getRequestURI().getPath();
        String[] parts = path.split("/");

        if (parts.length < 3) {

            sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный URI")));
            return;
        }
        int id = Integer.parseInt(parts[2]);
        Optional<Movie> movieId = store.getId(id);
        if (movieId.isPresent()) {
            String json = gson.toJson(movieId.get());
            sendJson(ex, 200, json);
        } else {

            sendJson(ex, 404, gson.toJson(new ErrorResponse("Фильм не найден")));
        }

    }

    private void handlePostRequest(HttpExchange ex) throws IOException {

        InputStream input = ex.getRequestBody();
        String body = new String(input.readAllBytes(), StandardCharsets.UTF_8);

        Movie movie = gson.fromJson(body, Movie.class);

        if (movie.getTitle() == null || movie.getTitle().isBlank()) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Название фильма не может быть пустым")));
            return;
        }
        if (movie.getTitle().length() > 50) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Название фильма должно быть не больше 50 символов")));
            return;
        }
        if (movie.getDescription() != null && movie.getDescription().length() > 600) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Описание фильма должно быть не больше 600 символов")));
            return;
        }
        if (movie.getYear() < 1800 || movie.getYear() > 2050) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Год выхода фильма должен быть в диапазоне от 1800 до 2050")));
            return;
        }

        Movie createdMovie = store.add(movie);
        String json = gson.toJson(createdMovie);
        sendJson(ex, 201, json);

    }

    private void handleDeleteRequest(HttpExchange ex) throws IOException {

        String path = ex.getRequestURI().getPath();
        String[] parts = path.split("/");

        if (parts.length < 3) {

            sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный URI")));
            return;
        }
        int id = Integer.parseInt(parts[2]);
        boolean deleted = store.delete(id);

        if (deleted) {
            sendNoContent(ex);
        } else {
            ErrorResponse error = new ErrorResponse("Фильм не найден");
            sendJson(ex, 404, gson.toJson(error));
        }
    }
}