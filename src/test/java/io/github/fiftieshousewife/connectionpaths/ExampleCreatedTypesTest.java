package io.github.fiftieshousewife.connectionpaths;

import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.REACHING;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.fiftieshousewife.connectionpaths.facts.Facts;
import org.junit.jupiter.api.Test;

// A call through an interface reaches only the implementations something in the code creates: one made only by name,
// from a setting no file gives, isn't followed, nor is one made only in the code of a class
// nothing creates, nor, for other test classes' tests, one only a test class creates.
class ExampleCreatedTypesTest {

    @Test
    void doesNotFollowACallToAClassNothingInTheCodeCreates() {
        assertThat(REACHING.get("com.example.plugins.ExportersTest.exports").getReaches())
            .extracting(Facts.Reach::getPoint)
            .doesNotContain("com.example.plugins.FtpExporter#export(String)");
    }

    @Test
    void doesNotFollowACallToAClassCreatedOnlyInCodeThatNeverRuns() {
        assertThat(REACHING).doesNotContainKey("com.example.groups.GroupsTest.listsMembers");
    }

    @Test
    void followsAClassATestCreatesOnlyForThatTestsClass() {
        assertThat(REACHING.get("com.example.groups.GroupsFromTheDirectoryTest.listsRemoteMembers").getReaches())
            .extracting(Facts.Reach::getPoint).containsExactly("com.example.groups.RemoteGroups#members(String)");
    }

    @Test
    void followsASuperCallOnlyIntoTheSuperclassNotIntoItsOtherSubclasses() {
        assertThat(REACHING).doesNotContainKey("com.example.jobs.LocalJobTest.prepares");
    }
}
