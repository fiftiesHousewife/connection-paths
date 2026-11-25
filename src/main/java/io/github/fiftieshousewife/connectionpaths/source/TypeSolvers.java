package io.github.fiftieshousewife.connectionpaths.source;

import io.github.fiftieshousewife.connectionpaths.Options;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;

/** Builds the symbol solver: the JDK, the source folders, then the classpath's jars. */
@Slf4j
public final class TypeSolvers {

    /**
     * The solver, what happened to each classpath entry that wasn't read as it was, and the major version of JUnit
     * Jupiter's API on the classpath, or 0 where it isn't there.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The solver is shared on purpose")
    public record Setup(CombinedTypeSolver solver, List<String> notes, int jupiter) {
        public Setup {
            notes = List.copyOf(notes);
        }
    }

    private static final Pattern JUPITER = Pattern.compile("junit-jupiter-api-(\\d+)\\..*\\.jar");

    private TypeSolvers() {
    }

    public static Setup create(final Options options) {
        // The JDK's own modules only, through the platform class loader: the libraries this tool runs with must not
        // stand in for the ones the code is built with.
        final CombinedTypeSolver solver =
            new CombinedTypeSolver(new ClassLoaderTypeSolver(ClassLoader.getPlatformClassLoader()));
        final List<Path> sources = new ArrayList<>(options.getMainSources());
        sources.addAll(options.getTestSources());
        // The source the solver reads resolves its own calls with the same solver, so code reached through it, such
        // as a field's initialiser in another class, can be followed further.
        final ParserConfiguration parsing = new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        sources.stream().filter(Files::isDirectory).forEach(dir -> solver.add(new JavaParserTypeSolver(dir, parsing)));
        final List<String> notes = new ArrayList<>();
        options.getClasspath().forEach(entry -> add(solver, entry, notes));
        parsing.setSymbolResolver(new JavaSymbolSolver(solver));
        return new Setup(solver, notes, jupiterVersion(options.getClasspath()));
    }

    private static void add(final CombinedTypeSolver solver, final Path entry, final List<String> notes) {
        if (Files.isDirectory(entry)) {
            notes.add("a class folder, not read (give the module's source with --main instead): " + shown(entry));
        } else if (!Files.isRegularFile(entry)) {
            notes.add("not found: " + shown(entry));
        } else {
            addJar(solver, entry, notes);
        }
    }

    private static int jupiterVersion(final List<Path> classpath) {
        return classpath.stream().map(entry -> JUPITER.matcher(String.valueOf(entry.getFileName())))
            .filter(Matcher::matches).mapToInt(m -> Integer.parseInt(m.group(1))).max().orElse(0);
    }

    // A path as the facts show one: from the folder the tool runs in, as the source files' are, so that no one's home
    // folder is written into them.
    private static String shown(final Path entry) {
        final Path here = Path.of("").toAbsolutePath();
        final Path absolute = entry.toAbsolutePath().normalize();
        final Path shown = Objects.equals(here.getRoot(), absolute.getRoot()) ? here.relativize(absolute) : absolute;
        return shown.toString().replace('\\', '/');
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
        final Path copy = Files.createTempFile("under-test-external-calls-", ".jar");
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
