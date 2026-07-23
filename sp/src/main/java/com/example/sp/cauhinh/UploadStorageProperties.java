package com.example.sp.cauhinh;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadStorageProperties {

    private String directory = "uploads";

    public Path directoryPath() {
        String configuredValue = directory == null || directory.isBlank()
                ? "uploads"
                : directory.trim();
        Path configuredPath = Path.of(configuredValue);
        if (configuredPath.isAbsolute()) {
            return configuredPath.toAbsolutePath().normalize();
        }
        return projectRoot().resolve(configuredPath).toAbsolutePath().normalize();
    }

    public String resourceLocation() {
        String location = directoryPath().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }

    private Path projectRoot() {
        Path workingDirectory = workingDirectory();
        Path detectedFromWorkingDirectory = findProjectRoot(workingDirectory);
        if (detectedFromWorkingDirectory != null) {
            return detectedFromWorkingDirectory;
        }

        try {
            URI codeLocationUri = UploadStorageProperties.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI();
            // Spring Boot executable jars expose classes through a ZIP/JAR
            // filesystem. Never mix that provider with a Windows filesystem
            // path; use the process working directory for packaged runs.
            if (!"file".equalsIgnoreCase(codeLocationUri.getScheme())) {
                return workingDirectory;
            }
            Path codeLocation = Path.of(codeLocationUri).toAbsolutePath().normalize();
            Path detectedFromClasses = findProjectRoot(
                    Files.isDirectory(codeLocation) ? codeLocation : codeLocation.getParent()
            );
            if (detectedFromClasses != null) {
                return detectedFromClasses;
            }
        } catch (Exception ignored) {
            // Fall back to the current directory only if the application location is unavailable.
        }
        return workingDirectory;
    }

    /**
     * Supports all common launch modes:
     * - Maven/JAR with the working directory at the module root;
     * - IDE with the working directory at the workspace root;
     * - IDE compiler output such as out/production/sp.
     */
    Path findProjectRoot(Path start) {
        Path current = start == null ? null : start.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
            Path moduleDirectory = current.resolve("sp");
            if (Files.isRegularFile(moduleDirectory.resolve("pom.xml"))) {
                return moduleDirectory;
            }
            current = current.getParent();
        }
        return null;
    }

    private Path workingDirectory() {
        return Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    }
}
