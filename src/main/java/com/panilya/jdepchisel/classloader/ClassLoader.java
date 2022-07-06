package com.panilya.jdepchisel.classloader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClassLoader {

    // Specify directory to search in
    // TODO: Implement CLI
    private final Path baseDir;

    public ClassLoader() {
        baseDir = Path.of("/home/&USER/programs/jdepchisel/src/test/java/com/panilya/jdepchisel");
    }

    public ByteBuffer loadClass(String name) {
        Path path = Path.of(baseDir.toString(), name, ".class");
        if (path.toFile().exists() && path.toFile().isFile()) {
            try {
                return ByteBuffer.wrap(Files.readAllBytes(path));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }
}
