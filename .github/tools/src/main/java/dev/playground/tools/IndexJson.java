package dev.playground.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class IndexJson {

    static final String rawGithubCDNPrefix = "https://raw.githubusercontent.com/java/playground-snippets/main/";

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("USAGE:\n\t[program] <path-to-snippets-folder>");
            System.exit(1);
        }
        var strPath = args[0];
        var path = Path.of(strPath);
        if (!path.toFile().exists()) {
            System.err.printf("Directory %s does not exist!\nUSAGE:\n\t[program] <path-to-snippets-folder>\n", path);
            System.exit(1);
        }

        String result;
        try(var walker = Files.walk(path)) {
            result = walker.filter(f -> !Files.isDirectory(f)).map(key -> {
                var strKey = key.toString().replace(strPath + "/", "");
                var strValue = rawGithubCDNPrefix + strKey;

                return String.format("\t\"%s\": \"%s\"", strKey, strValue);
            }).collect(Collectors.joining(",\n"));
        }
        var indexJsonContent = String.format("{\n%s\n}", result);
        var indexJsonFilePath = Path.of(strPath, "index.json");
        System.out.println(indexJsonFilePath);
        Files.deleteIfExists(indexJsonFilePath);
        indexJsonFilePath.toFile().createNewFile();

        Files.writeString(indexJsonFilePath, indexJsonContent);
        System.out.println(indexJsonContent);
    }
}
