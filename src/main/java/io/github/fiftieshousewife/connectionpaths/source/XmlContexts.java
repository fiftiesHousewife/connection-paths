package io.github.fiftieshousewife.connectionpaths.source;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Code that loads a Spring XML file as a context, and so makes its beans: a test class whose
 * {@code @ContextConfiguration} names the file, as its set-up, and a method that makes a
 * {@code ClassPathXmlApplicationContext}, {@code FileSystemXmlApplicationContext} or
 * {@code GenericXmlApplicationContext} on it. Each file is a step of its own,
 * {@code ApplicationContext#<xml applicationContext.xml>}, that calls each of its beans; files are matched by name.
 */
public final class XmlContexts {

    private static final Set<String> NAMING = Set.of("ContextConfiguration", "SpringJUnitConfig",
        "SpringJUnitWebConfig", "ImportResource");
    private static final Set<String> LOADING = Set.of("ClassPathXmlApplicationContext",
        "FileSystemXmlApplicationContext", "GenericXmlApplicationContext");

    private XmlContexts() {
    }

    /** The step that stands for a Spring XML file, matched by its name: {@code ApplicationContext#<xml app.xml>}. */
    public static MethodKey contextOf(final String file) {
        return new MethodKey("org.springframework.context.ApplicationContext", "<xml " + fileName(file) + ">", "");
    }

    // A test class's framework, and the Spring XML files its annotations name, which its set-up loads.
    static void read(final TypeDeclaration<?> type, final String name, final SourceModel model) {
        Frameworks.startedBy(type).ifPresent(annotation -> model.tests().startsAFramework(name, annotation));
        type.getAnnotations().stream().filter(a -> NAMING.contains(a.getName().getIdentifier()))
            .flatMap(XmlContexts::filesIn).forEach(file -> model.call(MethodKey.fieldsOf(name), contextOf(file)));
    }

    static List<String> loadedBy(final ObjectCreationExpr creation) {
        return LOADING.contains(creation.getType().getNameAsString())
            ? filesIn(creation).toList() : List.of();
    }

    private static Stream<String> filesIn(final Node node) {
        return node.findAll(StringLiteralExpr.class).stream().map(StringLiteralExpr::asString)
            .filter(text -> text.endsWith(".xml") && !text.contains("*"));
    }

    // A location's file name: classpath:spring/app.xml is app.xml.
    private static String fileName(final String location) {
        final String path = location.replace('\\', '/');
        return path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf(':')) + 1);
    }
}
