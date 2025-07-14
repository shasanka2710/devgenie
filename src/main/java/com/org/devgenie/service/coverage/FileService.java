package com.org.devgenie.service.coverage;

@Service
@Slf4j
public class FileService {

    public String readFile(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
    }

    public void writeFile(String filePath, String content) throws IOException {
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    public List<String> findJavaFiles(String directory) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(directory))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("/test/"))
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }
}
