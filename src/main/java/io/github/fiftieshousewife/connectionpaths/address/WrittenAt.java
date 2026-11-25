package io.github.fiftieshousewife.connectionpaths.address;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Where a literal address is written: its file, from the folder the tool runs in as the source files' are, and line.
 * It tells an address the main code holds from one a test gives.
 */
public record WrittenAt(String file, int line) {

    /** A literal address, with where it is written, if it doesn't say so already. */
    public static Address on(final Address address, final Node node) {
        if (!address.isLiteral() || address.getWrittenAt() != null) {
            return address;
        }
        return of(node).map(at -> address.toBuilder().writtenAt(at).build()).orElse(address);
    }

    // Literal parts joined, written where the first of them is, or else where the join is.
    static Address joined(final String value, final List<Address> parts, final Node join) {
        final WrittenAt first = parts.stream().filter(Address::isLiteral).map(Address::getWrittenAt)
            .filter(Objects::nonNull).findFirst().orElse(null);
        return on(Address.of(Address.LITERAL, value).toBuilder().writtenAt(first).build(), join);
    }

    private static Optional<WrittenAt> of(final Node node) {
        final Optional<Path> file = node.findCompilationUnit().flatMap(CompilationUnit::getStorage)
            .map(CompilationUnit.Storage::getPath);
        return file.flatMap(path -> node.getBegin().map(begin -> new WrittenAt(shown(path), begin.line)));
    }

    private static String shown(final Path file) {
        final Path here = Path.of("").toAbsolutePath();
        final boolean outside = !file.isAbsolute() || !Objects.equals(here.getRoot(), file.getRoot());
        return (outside ? file : here.relativize(file.normalize())).toString().replace('\\', '/');
    }
}
