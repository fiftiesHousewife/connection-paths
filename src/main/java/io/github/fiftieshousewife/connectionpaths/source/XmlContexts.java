package io.github.fiftieshousewife.connectionpaths.source;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Code that loads a Spring XML file as a context, and so makes its beans: a test class whose
 * {@code @ContextConfiguration} names the file, as its set-up, and a method that makes a
 * {@code ClassPathXmlApplicationContext}, {@code FileSystemXmlApplicationContext} or
 * {@code GenericXmlApplicationContext} on it. A JUnit 3 Spring test names its files in {@code getConfigLocations()},
 * where a plain one is at the classpath's root, or in {@code getConfigPaths()} or {@code getConfigPath()}, where it is
 * in the class's package. Each file is a step of its own,
 * {@code ApplicationContext#<xml applicationContext.xml>}, that calls each of its beans; files are matched by their
 * path on the classpath.
 */
public final class XmlContexts {

    private static final String CONTEXT = "org.springframework.context.ApplicationContext";
    private static final Set<String> NAMING = Set.of("ContextConfiguration", "SpringJUnitConfig",
        "SpringJUnitWebConfig", "ImportResource");
    private static final Set<String> LOADING = Set.of("ClassPathXmlApplicationContext",
        "FileSystemXmlApplicationContext", "GenericXmlApplicationContext");
    private static final String LOCATIONS = "getConfigLocations";
    private static final Set<String> PATHS = Set.of("getConfigPaths", "getConfigPath");

    private XmlContexts() {
    }

    /**
     * The step that stands for a Spring XML location, by its path on the classpath:
     * {@code classpath:spring/app.xml} is {@code ApplicationContext#<xml spring/app.xml>}. A location with a wildcard,
     * such as {@code classpath*:spring/*.xml}, keeps it, and stands for each file it matches.
     */
    public static MethodKey contextOf(final String location) {
        return new MethodKey(CONTEXT, "<xml " + pathOf(location) + ">", "");
    }

    /** The path on the classpath a step stands for, or empty if it stands for no Spring XML location. */
    public static Optional<String> locationOf(final MethodKey step) {
        return CONTEXT.equals(step.type()) && step.name().startsWith("<xml ")
            ? Optional.of(step.name().substring("<xml ".length(), step.name().length() - 1)) : Optional.empty();
    }

    // A test class's framework, and the Spring XML files its annotations or JUnit 3 methods name, which its set-up
    // loads; an annotation of the repo's own on it names the files that annotation's annotations do.
    static void read(final TypeDeclaration<?> type, final String name, final SourceModel model) {
        Frameworks.startedBy(type).ifPresent(annotation -> model.tests().startsAFramework(name, annotation));
        namedBy(type, name, model);
        type.getAnnotations().stream().filter(a -> !NAMING.contains(a.getName().getIdentifier()))
            .map(XmlContexts::qualifiedName).flatMap(Optional::stream)
            .forEach(annotation -> model.call(MethodKey.fieldsOf(name), MethodKey.fieldsOf(annotation)));
    }

    // The files a type's annotations or JUnit 3 methods name, which making it loads.
    private static void namedBy(final TypeDeclaration<?> type, final String name, final SourceModel model) {
        final Stream<String> named = type.getAnnotations().stream()
            .filter(a -> NAMING.contains(a.getName().getIdentifier())).flatMap(XmlContexts::filesIn);
        final Stream<String> paths = type.getMethods().stream()
            .filter(m -> PATHS.contains(m.getNameAsString()) && takesNothing(m)).flatMap(XmlContexts::filesIn);
        final Stream<String> locations = type.getMethodsByName(LOCATIONS).stream()
            .filter(XmlContexts::takesNothing).flatMap(XmlContexts::filesIn).map(XmlContexts::fromTheRoot);
        Stream.concat(Stream.concat(named, paths).map(file -> fromItsPackage(file, name)), locations)
            .forEach(file -> model.call(MethodKey.fieldsOf(name), contextOf(file)));
    }

    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidCatchingThrowable"})
    private static Optional<String> qualifiedName(final AnnotationExpr annotation) {
        try {
            return Optional.of(annotation.resolve().getQualifiedName());
        } catch (RuntimeException | StackOverflowError e) {
            return Optional.empty();
        }
    }

    private static boolean takesNothing(final MethodDeclaration method) {
        return method.getParameters().isEmpty();
    }

    private static String fromTheRoot(final String location) {
        return location.contains(":") || location.startsWith("/") ? location : "/" + location;
    }

    static List<String> loadedBy(final ObjectCreationExpr creation) {
        return LOADING.contains(creation.getType().getNameAsString())
            ? filesIn(creation).toList() : List.of();
    }

    private static Stream<String> filesIn(final Node node) {
        return node.findAll(StringLiteralExpr.class).stream().map(StringLiteralExpr::asString)
            .filter(text -> text.endsWith(".xml"));
    }

    // A test class's plain location, such as app.xml, is in its own package, as Spring reads it.
    private static String fromItsPackage(final String location, final String testClass) {
        final boolean plain = !location.contains(":") && !location.startsWith("/");
        final int dot = testClass.lastIndexOf('.');
        return plain && dot > 0 ? testClass.substring(0, dot).replace('.', '/') + "/" + location : location;
    }

    /**
     * A location's path on the classpath: without {@code classpath:}, {@code classpath*:} or {@code file:}, a leading
     * slash, or the folders up to a resources folder, as {@code src/main/resources/spring/app.xml} is
     * {@code spring/app.xml}.
     */
    public static String pathOf(final String location) {
        String path = location.replace('\\', '/').replaceFirst("^(classpath\\*?|file):", "");
        final int resources = path.lastIndexOf("resources/");
        path = resources >= 0 && (resources == 0 || path.charAt(resources - 1) == '/')
            ? path.substring(resources + "resources/".length()) : path;
        return path.replaceFirst("^/+", "");
    }
}
