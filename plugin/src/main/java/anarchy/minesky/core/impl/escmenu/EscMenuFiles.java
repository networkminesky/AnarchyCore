package anarchy.minesky.core.impl.escmenu;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EscMenuFiles {

    public static final String FILE_NAME = "esc-menu.yml";

    private EscMenuFiles() {}

    public static Path resolve(Path dataDirectory) {
        return dataDirectory.resolve(FILE_NAME);
    }

    public static void ensureDefaultFile(Path dataDirectory) throws IOException {
        Path target = resolve(dataDirectory);
        if (Files.exists(target)) {
            return;
        }
        Files.createDirectories(dataDirectory);
        try (InputStream defaults = EscMenuFiles.class.getResourceAsStream("/" + FILE_NAME)) {
            if (defaults == null) {
                throw new IOException("Recurso embutido ausente: " + FILE_NAME);
            }
            Files.copy(defaults, target);
        }
    }

    public static InputStream bundledDefaults() throws IOException {
        InputStream stream = EscMenuFiles.class.getResourceAsStream("/" + FILE_NAME);
        if (stream == null) {
            throw new IOException("Recurso embutido ausente: " + FILE_NAME);
        }
        return stream;
    }
}