package com.r.dependency;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PypiClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchesDependenciesFromRepositoryApi() throws IOException {
        startServer(200, """
                {"info":{"requires_dist":["certifi>=2024.0","idna>=3"]}}
                """);

        List<Dependency> dependencies = new PypiClient()
                .fetchDependencies(repositoryUrl(), "demo");

        assertEquals(2, dependencies.size());
        assertEquals("certifi", dependencies.get(0).name());
        assertEquals("idna>=3", dependencies.get(1).requirement());
    }

    @Test
    void reportsPackageNotFound() throws IOException {
        startServer(404, "{}");

        assertThrows(
                IllegalArgumentException.class,
                () -> new PypiClient().fetchDependencies(repositoryUrl(), "missing")
        );
    }

    private void startServer(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/pypi/demo/json", exchange -> {
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    private String repositoryUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }
}
