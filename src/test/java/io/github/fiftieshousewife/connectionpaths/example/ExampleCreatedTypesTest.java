package io.github.fiftieshousewife.connectionpaths.example;

import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.FACTS;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.REACHING;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.fiftieshousewife.connectionpaths.facts.Facts;
import io.github.fiftieshousewife.connectionpaths.facts.TestFacts;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

// A call through an interface reaches only the implementations something in the code creates: one made only by name,
// from a setting no file gives, isn't followed, nor is one made only in the code of a class
// nothing creates, nor, for other test classes' tests, one only a test class creates.
class ExampleCreatedTypesTest {

    @Test
    void doesNotFollowACallToAClassNothingInTheCodeCreates() {
        assertThat(REACHING.get("com.example.plugins.ExportersTest.exports").getReaches())
            .extracting(TestFacts.Reach::getPoint)
            .doesNotContain("com.example.plugins.FtpExporter#export(String)");
    }

    @Test
    void doesNotFollowACallToAClassCreatedOnlyInCodeThatNeverRuns() {
        assertThat(REACHING).doesNotContainKey("com.example.groups.GroupsTest.listsMembers");
    }

    @Test
    void followsAClassATestCreatesOnlyForThatTestsClass() {
        assertThat(REACHING.get("com.example.groups.GroupsFromTheDirectoryTest.listsRemoteMembers").getReaches())
            .extracting(TestFacts.Reach::getPoint).containsExactly("com.example.groups.RemoteGroups#members(String)");
    }

    @Test
    void listsOnlyTheSystemsATestsReachesConnectTo() {
        assertThat(REACHING.get("com.example.groups.GroupsTest.savesTheMembers").getSystems())
            .containsExactly("orders-db.internal");
    }

    @Test
    void namesForEachTestOnlySystemsOneOfItsReachesConnectsTo() {
        final Map<String, List<String>> points = FACTS.getSystems().stream()
            .collect(Collectors.toMap(Facts.ExternalSystem::getName, Facts.ExternalSystem::getPoints));
        assertThat(REACHING.values().stream().flatMap(test -> test.getSystems().stream()
            .filter(system -> test.getReaches().stream().map(TestFacts.Reach::getPoint)
                .noneMatch(points.get(system)::contains))
            .map(system -> test.getTest() + " " + system)).toList()).isEmpty();
    }

    @Test
    void followsASuperCallOnlyIntoTheSuperclassNotIntoItsOtherSubclasses() {
        assertThat(REACHING).doesNotContainKey("com.example.jobs.LocalJobTest.prepares");
    }
}
