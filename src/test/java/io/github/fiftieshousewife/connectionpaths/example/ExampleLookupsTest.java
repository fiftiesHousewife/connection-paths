package io.github.fiftieshousewife.connectionpaths.example;

import io.github.fiftieshousewife.connectionpaths.facts.Facts;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.FACTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Looking hosts up, forwards and in reverse, and a method that connects more than once. */
class ExampleLookupsTest {

    private static List<Facts.Point> pointsOf(final String method) {
        return FACTS.getConnectionPoints().stream().filter(p -> p.getMethod().equals(method)).toList();
    }

    @Test
    void findsReverseLookupsOfTheMachineAndOfAPeer() {
        assertThat(pointsOf("com.example.lookups.MachineName#canonicalName()"))
            .extracting(Facts.Point::getCall)
            .containsExactlyInAnyOrder("java.net.InetAddress#getLocalHost",
                "java.net.InetAddress#getCanonicalHostName");
    }

    @Test
    void findsAReverseLookupOfAPeersAddress() {
        assertThat(pointsOf("com.example.lookups.MachineName#peerName(Socket)"))
            .extracting(Facts.Point::getCall)
            .containsExactly("java.net.InetAddress#getHostName");
    }

    @Test
    void treatsALookupThroughJndiDnsAsLeavingTheMachine() {
        assertThat(pointsOf("com.example.lookups.ServiceRecords#find(String)"))
            .extracting(p -> p.getAddress().getSource(), Facts.Point::getLocal)
            .containsExactly(tuple("dns", null));
    }

    @Test
    void countsADnsLookupWhereItAsksForRecordsNotWhereItsContextIsMade() {
        assertThat(FACTS.getConnectionPoints()).filteredOn(p -> p.getMethod().startsWith("com.example.lookups.Service"))
            .extracting(Facts.Point::getMethod, Facts.Point::getCall)
            .containsExactly(tuple("com.example.lookups.ServiceRecords#find(String)",
                "javax.naming.directory.DirContext#getAttributes"));
    }

    @Test
    void listsEachCallOfAMethodThatConnectsMoreThanOnce() {
        assertThat(pointsOf("com.example.lookups.Dialler#open()"))
            .extracting(Facts.Point::getLine, p -> p.getAddress().getValue())
            .containsExactly(tuple(9, "primary.example.com"), tuple(11, "backup.example.com"));
    }

    @Test
    void takesALookupOfANumericAddressAsLookingNothingUp() {
        assertThat(pointsOf("com.example.lookups.Peers#gateway()")).extracting(Facts.Point::getLocal)
            .containsExactly("a numeric address, which nothing looks up");
    }

    @Test
    void gradesATestsLookupOfANumericAddressAsLocal() {
        assertThat(ExampleRepo.reachOf("com.example.lookups.PeersTest.findsAPeerByItsAddress", "host-lookup")
            .getAddressForTests()).isEqualTo("local");
    }
}
