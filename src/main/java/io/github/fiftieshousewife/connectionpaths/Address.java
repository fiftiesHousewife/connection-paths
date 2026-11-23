package io.github.fiftieshousewife.connectionpaths;

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

    static final String LITERAL = "literal";
    static final String SETTING = "setting";
    static final String ENVIRONMENT_VARIABLE = "environment-variable";
    static final String SYSTEM_PROPERTY = "system-property";
    static final String JNDI_NAME = "jndi-name";
    static final String CLASSPATH_RESOURCE = "classpath-resource";
    static final String LOCAL_FILE = "local-file";
    static final String DNS = "dns";
    static final String PARAMETER = "parameter";
    static final String BUILT_AT_RUN_TIME = "built-at-run-time";
    static final String UNKNOWN_PART = "{?}";

    private static final Set<String> NAMED = Set.of(SETTING, ENVIRONMENT_VARIABLE, SYSTEM_PROPERTY, JNDI_NAME);
    /** How much each source says about where a connection goes; a literal with no unknown parts says most. */
    private static final Map<String, Integer> RANKS = Map.of(SETTING, 8, JNDI_NAME, 7, ENVIRONMENT_VARIABLE, 7,
        SYSTEM_PROPERTY, 7, CLASSPATH_RESOURCE, 6, LOCAL_FILE, 6, DNS, 6, LITERAL, 5, PARAMETER, 2);
    private static final int FULL_LITERAL = 9;
    private static final int LEAST = 1;

    /** Most informative first, for choosing between the values a variable is given. */
    static final Comparator<Address> MOST_INFORMATIVE = Comparator.comparingInt(Address::rank).reversed();

    /** One of the constants above. */
    String source;
    /** The literal, the setting's, variable's or JNDI name, the parameter, or the code that builds it. */
    String value;
    /** For a setting: the method or annotation that reads it, such as {@code JiveGlobals.getProperty}. */
    String via;
    /** For a setting: its value in the repo's settings files, if it has one there. */
    String settingValue;
    /** For a setting: the file that holds that value. */
    String settingFile;
    /** For a setting: the line in that file. */
    int settingLine;
    /** For a setting: its value looks like a placeholder for a person to fill in. */
    boolean placeholder;

    static Address of(final String source, final String value) {
        return Address.builder().source(source).value(value).build();
    }

    static Address builtBy(final String code) {
        return of(BUILT_AT_RUN_TIME, code);
    }

    boolean isLiteral() {
        return LITERAL.equals(source);
    }

    boolean isFullLiteral() {
        return isLiteral() && !value.contains(UNKNOWN_PART);
    }

    /** Whether it names a setting, variable or JNDI name, by which a system can be known when its address isn't. */
    boolean isNamed() {
        return NAMED.contains(source);
    }

    private int rank() {
        return isFullLiteral() ? FULL_LITERAL : RANKS.getOrDefault(source, LEAST);
    }
}
