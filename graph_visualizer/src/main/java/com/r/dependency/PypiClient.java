package com.r.dependency;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class PypiClient {
    private final HttpClient httpClient;
    private final PypiJsonParser jsonParser;

    public PypiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.jsonParser = new PypiJsonParser();
    }

    /**
     * Получает список прямых зависимостей пакета из JSON API репозитория PyPI.
     */
    public List<Dependency> fetchDependencies(String repository, String packageName) {
        URI uri = createPackageUri(repository, packageName);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("User-Agent", "graph-visualizer/1.0")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() == 404) {
                throw new IllegalArgumentException(
                        "Пакет не найден в репозитории: " + packageName
                );
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Репозиторий вернул HTTP " + response.statusCode()
                );
            }
            return jsonParser.parseDependencies(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Запрос к репозиторию был прерван.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Не удалось получить данные из репозитория: " + exception.getMessage(),
                    exception
            );
        }
    }

    private URI createPackageUri(String repository, String packageName) {
        String baseUrl = repository.endsWith("/")
                ? repository.substring(0, repository.length() - 1)
                : repository;
        String encodedName = URLEncoder.encode(packageName, StandardCharsets.UTF_8);
        return URI.create(baseUrl + "/pypi/" + encodedName + "/json");
    }
}
