package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.source.Unresolved;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * What the tool found, written as JSON with snake_case names. Its schema is
 * {@code schema/external-calls.schema.json} in the under-test toolkit.
 */
@Value
@Builder
public class Facts {

    static final String SCHEMA = "external-calls/1";
    static final String FRAMEWORK_TESTS = "tests_starting_a_framework";

    @Builder.Default
    String schema = SCHEMA;
    String toolVersion;
    Summary summary;
    @Singular
    List<Point> connectionPoints;
    @Singular
    List<ExternalSystem> systems;
    @Singular
    List<TestReach> tests;
    @Singular("testStartingAFramework")
    @JsonProperty(FRAMEWORK_TESTS)
    List<FrameworkTest> testsStartingAFramework;
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
        @JsonProperty(FRAMEWORK_TESTS)
        int testsStartingAFramework;
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
        Address address;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String local;
    }

    /**
     * Something outside the program that tests may reach: the connection points that go to one host, or failing
     * that one setting, variable or JNDI name, or failing that one class.
     */
    @Value
    @Builder
    public static class ExternalSystem {
        String name;
        String namedBy;
        @Singular
        List<String> kinds;
        Address address;
        @Singular
        List<String> points;
        @Singular
        List<String> alternatives;
        int tests;
        @Singular("sharedStep")
        List<String> sharedChain;
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
        @Singular
        List<String> systems;
        @Singular
        List<String> standIns;
    }

    /**
     * A test that starts a framework, such as Spring or Quarkus, or containers: the framework may open connections
     * that no call in the source shows, whether or not the test is also in {@code tests}.
     */
    @Value
    @Builder
    public static class FrameworkTest {
        String test;
        String file;
        int line;
        String framework;
        String annotatedOn;
    }

    /** One kind of connection a test may reach, and one chain of calls showing how. */
    @Value
    @Builder(toBuilder = true)
    public static class Reach {
        String kind;
        String point;
        boolean throughSetUp;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String throughMock;
        String addressForTests;
        @Singular("step")
        List<String> chain;
    }

    /** A call through a supertype with more overriding methods than the limit: reported, not followed. */
    public record Dispatch(String method, int implementations) {
    }
}
