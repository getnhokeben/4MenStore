package com.example.sp.cauhinh;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UploadStoragePropertiesTest {

    @TempDir
    Path tempDirectory;

    @Test
    void findsModuleFromWorkspaceAndIdeOutputDirectories() throws Exception {
        Path module = Files.createDirectories(tempDirectory.resolve("sp"));
        Files.createFile(module.resolve("pom.xml"));
        Path ideOutput = Files.createDirectories(tempDirectory.resolve("out/production/sp"));

        UploadStorageProperties properties = new UploadStorageProperties();

        assertEquals(module, properties.findProjectRoot(tempDirectory));
        assertEquals(module, properties.findProjectRoot(ideOutput));
    }
}
