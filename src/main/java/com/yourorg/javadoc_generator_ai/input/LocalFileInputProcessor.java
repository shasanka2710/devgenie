package com.yourorg.javadoc_generator_ai.input;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LocalFileInputProcessor {
    public List<File> processInput(String source) throws IOException {
        File file = new File(source);
        if (!file.exists()) {
            throw new IllegalArgumentException("Source path does not exist: " + source);
        }

        if (file.isDirectory()) {
            return Files.walk(Paths.get(source))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } else if (file.isFile() && source.endsWith(".java")) {
            List<File> files = new ArrayList<>();
            files.add(file);
            return files;
        } else {
            throw new IllegalArgumentException("Invalid input source: " + source);
        }
    }
}
