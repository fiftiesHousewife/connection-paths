package io.github.fiftieshousewife.connectionpaths;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * What the tool found, written as JSON with snake_case names. Its schema is
 * {@code schema/connection-paths.schema.json} in the under-test toolkit.
 */
@Value
@Builder
public class Facts {

    static final String SCHEMA = "connection-paths/1";

    @Builder.Default
    String schema = SCHEMA;
    String toolVersion;
    Summary summary;
    @Singular
    List<Point> connectionPoints;
    @Singular
    List<TestReach> tests;
    @Singular("notResolved")
    List<Unresolved.Reason> notResolved;
    @Singular("dispatchPointNotFollowed")
    List<Dispatch> dispatchPointsNotFollowed;
    @Singular
    List<String> classpathNotes;

    /** How much was read, and how much of it resolved. */
    @Value
    @Builder
    public static class Summary {
        int files;
        int filesNotParsed;
        int methods;
        int callsResolved;
        int callsNotResolved;
        int tests;
        int testsReaching;
    }

    /** A method that opens or uses a connection, and the call that does it. */
    @Value
    @Builder
    public static class Point {
        String kind;
        String call;
        String method;
        String file;
        int line;
    }

    /** A test that may reach one or more kinds of connection. */
    @Value
    @Builder
    public static class TestReach {
        String test;
        String file;
        int line;
        @Singular("reach")
        List<Reach> reaches;
    }

    /** One kind of connection a test may reach, and one chain of calls showing how. */
    @Value
    @Builder
    public static class Reach {
        String kind;
        String point;
        boolean throughSetUp;
        @Singular("step")
        List<String> chain;
    }

    /** A call through a supertype with more overriding methods than the limit: reported, not followed. */
    public record Dispatch(String method, int implementations) {
    }
}
