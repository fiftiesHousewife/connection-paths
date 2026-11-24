package io.github.fiftieshousewife.connectionpaths;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    static final String SCHEMA = "connection-paths/3";

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
        /** Where the address the call connects to comes from, as far as the source shows. */
        Address address;
        /** Why the connection stays on this machine, if it does: then it is part of no system. */
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
        /** host, setting, environment-variable, system-property, jndi-name or class. */
        String namedBy;
        @Singular
        List<String> kinds;
        /** The address of its first connection point. */
        Address address;
        /** The methods that open or use the connection. */
        @Singular
        List<String> points;
        /**
         * Systems reached through the same interface or superclass method: implementations a program chooses between,
         * so a test reaches one of them, not all.
         */
        @Singular
        List<String> alternatives;
        /** How many tests may reach it. */
        int tests;
        /** The calls every one of those tests goes through on its way to the system, nearest the test first. */
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
        /** The systems it may reach. */
        @Singular
        List<String> systems;
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
        /** The annotation that starts it, as written: {@code @SpringBootTest}. */
        String framework;
        /** The class the annotation is on: the test's own, a superclass or a class around it. */
        String annotatedOn;
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
