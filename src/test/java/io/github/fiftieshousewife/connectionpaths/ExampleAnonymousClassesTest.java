package io.github.fiftieshousewife.connectionpaths;

import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.OPEN;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.OPEN_QUIETLY;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.REACHING;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.onlyReach;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// The example repo's anonymous class, made in a static initialiser: its method runs when it is called through its
// interface, not when the class that makes it is loaded.
class ExampleAnonymousClassesTest {

    private static final String WATCHERS = "com.example.watch.Watchers";

    @Test
    void doesNotRunAnAnonymousClassesMethodsWhereItIsMade() {
        assertThat(REACHING).doesNotContainKey("com.example.watch.WatchersTest.countsTheWatchers");
    }

    @Test
    void reachesAnAnonymousClassesMethodThroughItsInterface() {
        assertThat(onlyReach("com.example.watch.WatchersTest.firesAChange").getChain()).containsExactly(
            "com.example.watch.WatchersTest#firesAChange()",
            WATCHERS + "#fire(String)",
            WATCHERS + "$1#changed(String)",
            OPEN_QUIETLY,
            OPEN);
    }
}
