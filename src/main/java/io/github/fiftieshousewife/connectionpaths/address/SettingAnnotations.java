package io.github.fiftieshousewife.connectionpaths.address;

import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The settings Spring reads into a field: {@code @Value("${orders.db.url}")} on it, or {@code orders-db.<field>} for a
 * field of a class annotated {@code @ConfigurationProperties("orders-db")}.
 */
public final class SettingAnnotations {

    private static final Pattern PLACEHOLDER = Pattern.compile("^\\$\\{([^}:]+)(:.*)?}$");
    private static final Set<String> PREFIX_MEMBERS = Set.of("prefix", "value");

    private SettingAnnotations() {
    }

    /** The key in a Spring placeholder, {@code ${key}} or {@code ${key:default}}. */
    public static Optional<String> keyOf(final String text) {
        final Matcher matcher = PLACEHOLDER.matcher(text);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    /** The default in a Spring placeholder, {@code ${key:default}}, used where nothing sets the key. */
    public static Optional<String> defaultOf(final String text) {
        final Matcher matcher = PLACEHOLDER.matcher(text);
        return matcher.matches() && matcher.group(2) != null ? Optional.of(matcher.group(2).substring(1))
            : Optional.empty();
    }

    /**
     * A setting given as {@code ${key}}, read by {@code via}, with its default as its value until a settings file sets
     * it; otherwise the text as a literal address.
     */
    public static Address settingOrLiteral(final String text, final String via) {
        return keyOf(text).map(key -> Address.builder().source(Address.SETTING).value(key).via(via)
                .settingValue(defaultOf(text).orElse(null)).build())
            .orElse(Address.of(Address.LITERAL, text));
    }

    static Optional<Address> ofField(final TypeDeclaration<?> type, final String id) {
        return type.getFields().stream()
            .filter(f -> f.getVariables().stream().anyMatch(v -> v.getNameAsString().equals(id)))
            .flatMap(f -> f.getAnnotations().stream())
            .map(SettingAnnotations::fromValue)
            .flatMap(Optional::stream)
            .findFirst()
            .or(() -> bound(type, id));
    }

    private static Optional<Address> fromValue(final AnnotationExpr annotation) {
        final boolean isValue = "Value".equals(annotation.getName().getIdentifier());
        return isValue && annotation instanceof SingleMemberAnnotationExpr single
            && single.getMemberValue() instanceof StringLiteralExpr text
            ? Optional.of(settingOrLiteral(text.asString(), "@Value")) : Optional.empty();
    }

    private static Optional<Address> bound(final TypeDeclaration<?> type, final String id) {
        if (type.getFieldByName(id).isEmpty()) {
            return Optional.empty();
        }
        return type.getAnnotationByName("ConfigurationProperties").flatMap(SettingAnnotations::prefixOf)
            .map(prefix -> Address.builder().source(Address.SETTING).value(prefix + "." + id)
                .via("@ConfigurationProperties").build());
    }

    private static Optional<String> prefixOf(final AnnotationExpr annotation) {
        if (annotation instanceof SingleMemberAnnotationExpr single) {
            return Declarations.constant(single.getMemberValue());
        }
        return annotation instanceof NormalAnnotationExpr normal ? normal.getPairs().stream()
            .filter(pair -> PREFIX_MEMBERS.contains(pair.getNameAsString())).findFirst()
            .flatMap(pair -> Declarations.constant(pair.getValue())) : Optional.empty();
    }
}
