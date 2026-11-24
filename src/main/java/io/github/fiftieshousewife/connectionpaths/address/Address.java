package io.github.fiftieshousewife.connectionpaths.address;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

/**
 * Where a connection's address comes from, as far as the source shows: a literal, a setting, an environment variable,
 * a system property, a JNDI name, a local resource, a parameter, or something built at run time. A literal with parts
 * that can't be known holds {@code {?}} for each.
 */
@Value
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class Address {

    public static final String LITERAL = "literal";
    public static final String SETTING = "setting";
    public static final String DATA_SOURCE_URL = "spring.datasource.url";
    public static final String ENVIRONMENT_VARIABLE = "environment-variable";
    static final String SYSTEM_PROPERTY = "system-property";
    public static final String JNDI_NAME = "jndi-name";
    static final String CLASSPATH_RESOURCE = "classpath-resource";
    static final String LOCAL_FILE = "local-file";
    public static final String DNS = "dns";
    public static final String PARAMETER = "parameter";
    public static final String BUILT_AT_RUN_TIME = "built-at-run-time";
    static final String UNKNOWN_PART = "{?}";

    private static final Set<String> NAMED = Set.of(SETTING, ENVIRONMENT_VARIABLE, SYSTEM_PROPERTY, JNDI_NAME);
    private static final Map<String, Integer> RANKS = Map.of(SETTING, 8, JNDI_NAME, 7, ENVIRONMENT_VARIABLE, 7,
        SYSTEM_PROPERTY, 7, CLASSPATH_RESOURCE, 6, LOCAL_FILE, 6, DNS, 6, LITERAL, 5, PARAMETER, 2);
    private static final int FULL_LITERAL = 9;
    private static final int LEAST = 1;

    public static final Comparator<Address> MOST_INFORMATIVE = Comparator.comparingInt(Address::rank).reversed();

    String source;
    String value;
    String via;
    String settingValue;
    String settingFile;
    int settingLine;
    boolean placeholder;

    public static Address of(final String source, final String value) {
        return Address.builder().source(source).value(value).build();
    }

    public static Address builtBy(final String code) {
        return of(BUILT_AT_RUN_TIME, code);
    }

    @JsonIgnore
    public boolean isLiteral() {
        return LITERAL.equals(source);
    }

    @JsonIgnore
    boolean isFullLiteral() {
        return isLiteral() && !value.contains(UNKNOWN_PART);
    }

    /** Whether it names a setting, variable or JNDI name, by which a system can be known when its address isn't. */
    @JsonIgnore
    public boolean isNamed() {
        return NAMED.contains(source);
    }

    private int rank() {
        return isFullLiteral() ? FULL_LITERAL : RANKS.getOrDefault(source, LEAST);
    }
}
