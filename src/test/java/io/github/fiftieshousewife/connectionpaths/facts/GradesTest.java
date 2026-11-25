package io.github.fiftieshousewife.connectionpaths.facts;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class GradesTest {

    @Test
    void takesAWebServerByItsPackageAndOtherServersByTheirName() {
        assertThat(List.of(
                Grades.standsInFor("io.undertow.Undertow", "http"),
                Grades.standsInFor("reactor.netty.http.server.HttpServer", "url"),
                Grades.standsInFor("io.netty.channel.EventLoopGroup", "http"),
                Grades.standsInFor("com.example.Server", "http"),
                Grades.standsInFor("org.testcontainers.containers.MySQLContainer", "database"),
                Grades.standsInFor("org.testcontainers.kafka.KafkaContainer", "database")))
            .containsExactly(true, true, false, false, true, false);
    }
}
