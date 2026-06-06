package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoviesHandlerTest {
    private static final String BASE = "http://localhost:8081";
    private static HttpClient client = HttpClient.newBuilder().build();
    private static MoviesServer server;
    private static MoviesStore store;

    @BeforeAll
    static void beforeAll() {
        try {
            store = new MoviesStore();

            server = new MoviesServer(store, 8081);
            server.start();
        } catch (Exception e) {
            System.err.println("Не удалось запустить сервер: " + e.getMessage());
            throw new RuntimeException("Сервер не был запущен. Тесты не выполнены");
        }
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @Test
    void postMovie_shouldCreateMovieAndReturn() throws Exception {
        String body = """
                {"title": "Inception", "description": "Thriller"}""";


        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("Inception"));
        assertTrue(response.body().contains("id"));
    }

    @Test
    void getMovieById_shouldReturnMovie_whenExists() throws Exception {
        String createBody = """
                {"title": "Interstellar", "description": "Space exploration movie"}
                """;

        HttpRequest createRequest = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(createBody)).build();
        HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String responseBody = createResponse.body();
        int id = Integer.parseInt(responseBody.split("\"id\":")[1].split(",")[0].trim());

        HttpRequest getRequest = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/" + id)).GET().build();
        HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, getResponse.statusCode());
        assertTrue(getResponse.body().contains("Interstellar"));
    }

    @Test
    void deleteMovie_shouldReturn_whenDeleted() throws Exception {

        String createBody = """
                {"title": "The Matrix", "description": "Sci-fi classic"}
                """;

        HttpRequest createRequest = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(createBody)).build();

        HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        int id = Integer.parseInt(createResponse.body().split("\"id\":")[1].split(",")[0].trim());
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/" + id)).DELETE().build();
        HttpResponse<String> deleteResponse = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(204, deleteResponse.statusCode());
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());
        assertEquals("application/json; charset=UTF-8", response.headers().firstValue("Content-Type").orElse(""));


        Gson gson = new Gson();
        List<Movie> movies = gson.fromJson(response.body(), new ListOfMoviesTypeToken().getType());

        assertNotNull(movies);
        assertTrue(movies.isEmpty(), "Список фильмов должен быть пустым");
    }
}