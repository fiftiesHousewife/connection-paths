package io.github.fiftieshousewife.connectionpaths.facts;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/** What the facts say of each test that may reach a connection: how it reaches each, and whether it connects. */
public final class TestFacts {

    private TestFacts() {
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
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String startsFramework;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String testFramework;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<String> settingsFiles;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<String> profiles;
    }

    /** One kind of connection a test may reach, and one chain of calls showing how. */
    @Value
    @Builder(toBuilder = true)
    public static class Reach {
        String kind;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String system;
        String point;
        boolean throughSetUp;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String throughMock;
        String addressForTests;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String host;
        String grade;
        String reason;
        String reasonText;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        CallSite firstCall;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String clientField;
        @Singular("step")
        List<String> chain;
    }

    /** Where a call is made: its file, as the source files are named, and line. */
    @Value
    @Builder
    public static class CallSite {
        String file;
        int line;
    }
}
