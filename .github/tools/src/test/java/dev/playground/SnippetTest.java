package dev.playground;

import dev.playground.tools.IndexJson;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SnippetTest {

    static final HttpRequest.Builder builder = HttpRequest.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .headers("Content-Type", "application/x-www-form-urlencoded");

    static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(300))
            .version(HttpClient.Version.HTTP_1_1)
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .build();

    static final String bodyTemplate = """
            {
                "snippet": "%s",
                "option": "na"
            }
            """;

    static List<String> getData() throws URISyntaxException {
        List<String> data;
        var mainClassLoader = IndexJson.class.getClassLoader();
        var samplesFolder = Path.of(Objects.requireNonNull(mainClassLoader.getResource("snippets")).toURI());
        try(var files = Files.walk(samplesFolder)) {
            data = files.filter(fl -> fl.toString().endsWith(".snippet")).map(f -> {
                String snippet;
                var snippetPath = f.getParent() + "/" + f.getFileName();
                try {
                    snippet = new String(Files.readAllBytes(Path.of(snippetPath)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return snippet;
            }).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return data;
    }

    String executeSnippet(String snippet) throws Exception{
        var publicEndpoint = System.getProperty("execution.endpoint");
        Assumptions.assumeTrue(Objects.nonNull(publicEndpoint));
        var requestBody = bodyTemplate.formatted(
                new String(Base64.getEncoder().encode(snippet.getBytes(StandardCharsets.UTF_8))
                ));
        var request = builder.uri(new URI(publicEndpoint))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        var responseBody = new String(response.body());
        assertEquals(200, response.statusCode(), responseBody);

        assertTrue(responseBody.contains("VALID"), responseBody);


        return responseBody;
    }

    @ParameterizedTest
    @MethodSource(value = "getData")
    public void testCompile(String snippet) throws Exception {
        System.out.printf("[SnippetTest::testCompile] Running validation test for snippet:\n%s\n", snippet);
        executeSnippet(snippet);
    }

    @Test
    public void testCharset() throws Exception {
        var snippet = """
                System.out.println("tu dois faire ça");
                """;
        System.out.printf("[SnippetTest::testCharset] Running charset validation test for snippet:\n%s\n", snippet);
        var responseBody = executeSnippet(snippet);
        assertTrue(responseBody.contains("tu dois faire ça"));
    }
}
