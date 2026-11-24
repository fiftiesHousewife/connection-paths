package io.github.fiftieshousewife.connectionpaths;

import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.FACTS;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.REACHING;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.addressOf;
import static io.github.fiftieshousewife.connectionpaths.ExampleRepo.system;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;

/** The example repo's clients that are only an annotated interface, made at run time by a library. */
class ExampleClientsTest {

    @Test
    void findsCallsToAnInterfaceOrMethodCarryingAClientAnnotation() {
        assertThat(FACTS.getConnectionPoints()).filteredOn(point -> point.getMethod().startsWith("com.example.clients"))
            .extracting(Facts.Point::getMethod, Facts.Point::getCall)
            .containsExactlyInAnyOrder(
                tuple("com.example.clients.Shop#orderCount()", "@org.springframework.cloud.openfeign.FeignClient"),
                tuple("com.example.clients.Shop#priceList()", "@retrofit2.http.GET"));
    }

    @Test
    void leavesOutAnUnannotatedDefaultMethodAndAServersOwnAnnotation() {
        assertAll(
            () -> assertThat(REACHING).doesNotContainKey("com.example.clients.ShopTest.namesTheCurrency"),
            () -> assertThat(REACHING).doesNotContainKey("com.example.clients.ShopTest.countsStock"),
            () -> assertThat(REACHING).containsKey("com.example.clients.ShopTest.countsOrders"));
    }

    @Test
    void takesTheAddressFromTheAnnotationsUrl() {
        final Address orders = addressOf("com.example.clients.Shop#orderCount()");
        assertAll(
            () -> assertThat(orders.getValue()).isEqualTo("orders.url"),
            () -> assertThat(orders.getVia()).isEqualTo("@FeignClient"),
            () -> assertThat(orders.getSettingValue()).isEqualTo("https://orders-api.internal/v2"),
            () -> assertThat(system("orders-api.internal").getPoints())
                .contains("com.example.clients.Shop#orderCount()"),
            () -> assertThat(addressOf("com.example.clients.Shop#priceList()").getSource())
                .isEqualTo(Address.BUILT_AT_RUN_TIME));
    }
}
