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

    }

    private void handleGetRequest(HttpExchange ex) throws IOException {
        try {
            List<Movie> movies = store.getAll();
            String json = gson.toJson(movies);
            sendJson(ex, 200, json);

        } catch (Exception e) {

            sendJson(ex, 500, gson.toJson(new ErrorResponse("Внутренняя ошибка сервера")));
        }
    }

    private void handleGetByIdRequest(HttpExchange ex) throws IOException {
        try {
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
        } catch (NumberFormatException e) {

            sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный идентификатор фильма")));
        } catch (Exception e) {
            sendJson(ex, 500, gson.toJson(new ErrorResponse("Внутренняя ошибка сервера")));
        }
    }

    private void handlePostRequest(HttpExchange ex) throws IOException {
        try {
            InputStream input = ex.getRequestBody();
            String body = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            Movie movie = gson.fromJson(body, Movie.class);

            if (movie.getTitle() == null || movie.getTitle().isBlank()) {
                sendJson(ex, 400, gson.toJson(new ErrorResponse("Название фильма не может быть пустым")));
                return;
            }
            Movie createdMovie = store.add(movie);
            String json = gson.toJson(createdMovie);
            sendJson(ex, 201, json);
        } catch (Exception e) {

            sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректные данные")));
        }
    }

    private void handleDeleteRequest(HttpExchange ex) throws IOException {
        try {
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

        } catch (NumberFormatException e) {

            sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный идентификатор фильма")));
        } catch (Exception e) {

            sendJson(ex, 500, gson.toJson(new ErrorResponse("Внутренняя ошибка сервера")));
        }
    }

}