package io.github.fiftieshousewife.connectionpaths;

import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;

/** Builds the symbol solver: the JDK, the source folders, then the classpath's jars and class folders. */
@Slf4j
final class TypeSolvers {

    /** The solver, and what happened to each classpath entry that wasn't read as it was. */
    record Setup(CombinedTypeSolver solver, List<String> notes) {
        Setup {
            notes = List.copyOf(notes);
        }
    }

    private TypeSolvers() {
    }

    static Setup create(final Options options) {
        final CombinedTypeSolver solver = new CombinedTypeSolver(new ReflectionTypeSolver(false));
        final List<Path> sources = new ArrayList<>(options.getMainSources());
        sources.addAll(options.getTestSources());
        sources.stream().filter(Files::isDirectory).forEach(dir -> solver.add(new JavaParserTypeSolver(dir)));
        final List<String> notes = new ArrayList<>();
        for (final Path entry : options.getClasspath()) {
            add(solver, entry, notes);
        }
        return new Setup(solver, notes);
    }

    private static void add(final CombinedTypeSolver solver, final Path entry, final List<String> notes) {
        if (Files.isDirectory(entry)) {
            notes.add("a class folder, not read (give the module's source with --main instead): " + entry);
        } else if (!Files.isRegularFile(entry)) {
            notes.add("not found: " + entry);
        } else {
            addJar(solver, entry, notes);
        }
    }

    // JavaParser fails on some jars' module-info.class; a copy without it reads fine.
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private static void addJar(final CombinedTypeSolver solver, final Path jar, final List<String> notes) {
        try {
            solver.add(new JarTypeSolver(jar));
        } catch (IOException | RuntimeException first) {
            try {
                solver.add(new JarTypeSolver(withoutModuleInfo(jar)));
                notes.add("read without its module-info: " + jar.getFileName());
            } catch (IOException | RuntimeException second) {
                log.debug("Can't read {}", jar, second);
                notes.add("could not read: " + jar.getFileName() + " (" + second.getMessage() + ")");
            }
        }
    }

    static Path withoutModuleInfo(final Path jar) throws IOException {
        final Path copy = Files.createTempFile("connection-paths-", ".jar");
        copy.toFile().deleteOnExit();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(jar));
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(copy))) {
            int copied = 0;
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                if (!entry.getName().endsWith("module-info.class")) {
                    copyEntry(in, out, entry.getName());
                    copied++;
                }
            }
            if (copied == 0) {
                throw new IOException("not a jar, or one with nothing in it");
            }
        }
        return copy;
    }

    private static void copyEntry(final InputStream in, final ZipOutputStream out, final String name) {
        try {
            out.putNextEntry(new ZipEntry(name));
            in.transferTo((OutputStream) out);
            out.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
