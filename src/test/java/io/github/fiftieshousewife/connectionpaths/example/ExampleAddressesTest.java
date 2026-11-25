package io.github.fiftieshousewife.connectionpaths.example;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.facts.Facts;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.FACTS;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.addressOf;
import static io.github.fiftieshousewife.connectionpaths.example.ExampleRepo.assertAddress;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;

/** Where the example repo's connections go: each address traced to its source, and those that stay on the machine. */
class ExampleAddressesTest {

    @Test
    void givesAPointWhoseAddressIsAParameterWhatTheCodeCallingItPasses() {
        assertAddress("com.example.directory.DirectoryContext#<init>(Hashtable)", Address.SETTING, "ldap.host");
    }

    @Test
    void tracesEachAddressToWhereItComesFrom() {
        assertAll(
            () -> assertAddress("com.example.db.Database#open()", Address.LITERAL,
                "jdbc:postgresql://orders-db.internal/orders"),
            () -> assertAddress("com.example.addresses.CrmClient#open()", Address.SETTING, "crm.host"),
            () -> assertAddress("com.example.addresses.PaymentsClient#balances()", Address.ENVIRONMENT_VARIABLE,
                "PAYMENTS_URL"),
            () -> assertAddress("com.example.addresses.OrdersLookup#connect(InitialContext)", Address.JNDI_NAME,
                "java:comp/env/jdbc/orders"),
            () -> assertAddress("com.example.addresses.PoolFactory#connect(SimpleDataSource)", Address.LITERAL,
                "jdbc:mysql://pool-db.internal/app"),
            () -> assertAddress("com.example.addresses.RegionClient#prices(String)", Address.LITERAL,
                "https://{?}.prices.internal/v1"),
            () -> assertAddress("com.example.docs.DocumentReader#read(URL)", Address.PARAMETER, "url of read"),
            () -> assertAddress("com.example.tools.Shell#list()", Address.LITERAL, "ls"),
            () -> assertAddress("com.example.addresses.Notifier.Sender#open()", Address.ENVIRONMENT_VARIABLE,
                "NOTIFY_HOST"),
            () -> assertThat(addressOf("com.example.addresses.SecureChannel#open()").getSource())
                .isEqualTo(Address.BUILT_AT_RUN_TIME));
    }

    @Test
    void takesNoProtocolNameFromWhatACallIsMadeOnForAnAddress() {
        assertAddress("com.example.addresses.SecureUpgrade#upgrade(Socket,String)", Address.PARAMETER,
            "host of upgrade");
    }

    // A literal says where it is written, so an address the main code holds is told from one a test gives.
    @Test
    void saysWhereEachLiteralAddressIsWritten() {
        assertThat(FACTS.getConnectionPoints().stream()
                .filter(point -> point.getMethod().equals("com.example.db.Database#open()")
                    || point.getAddress().getValue().equals("ftp-mirror.internal"))
                .map(point -> point.getAddress().getWrittenAt())
                .map(at -> at.file().substring(at.file().indexOf("/src/") + 1) + ":" + at.line()))
            .containsExactlyInAnyOrder("src/main/java/com/example/db/Database.java:9",
                "src/test/java/org/apache/commons/net/SocketClientTest.java:11");
    }

    @Test
    void followsABuilderChainAndAMethodsReturnToTheSettingItReads() {
        final Address directory = addressOf("com.example.addresses.DirectoryClient#users()");
        assertAll(
            () -> assertThat(directory.getSource()).isEqualTo(Address.SETTING),
            () -> assertThat(directory.getValue()).isEqualTo("directory.server.url"),
            () -> assertThat(directory.getVia()).isEqualTo("props.getProperty"));
    }

    @Test
    void takesAnAddressFromWhatACallIsMadeOnWhenItsArgumentIsNotOne() {
        assertThat(FACTS.getConnectionPoints())
            .filteredOn(point -> point.getCall().equals("javax.mail.Transport#sendMessage"))
            .extracting(point -> point.getAddress().getSource())
            .containsExactly(Address.BUILT_AT_RUN_TIME);
    }

    @Test
    void looksASettingUpInTheRepositorysPropertiesYamlAndXmlFiles() {
        final Address crm = addressOf("com.example.addresses.CrmClient#open()");
        final Address ledger = addressOf("com.example.addresses.LedgerStore#open()");
        assertAll(
            () -> assertThat(crm.getSettingValue()).isEqualTo("crm.internal"),
            () -> assertThat(crm.getSettingFile()).endsWith("application.properties"),
            () -> assertThat(crm.getSettingLine()).isEqualTo(2),
            () -> assertThat(crm.getVia()).isEqualTo("Settings.get"),
            () -> assertThat(ledger.getValue()).isEqualTo("database.serverURL"),
            () -> assertThat(ledger.getSettingValue()).isEqualTo("jdbc:postgresql://ledger-db.internal/ledger"));
    }

    @Test
    void takesASettingFromTheFileWithoutAProfileOverOneWithAProfile() {
        assertThat(addressOf("com.example.addresses.CrmClient#open()").getSettingFile())
            .endsWith("/application.properties");
    }

    @Test
    void tracesAFieldNotALocalVariableOfTheSameNameElsewhere() {
        assertThat(addressOf("com.example.addresses.StatusClient#check()").getValue())
            .isEqualTo("https://status.internal/health");
    }

    @Test
    void leavesOutASettingOnlyAProfilesFileGives() {
        assertThat(addressOf("com.example.addresses.PriceFeeds#rates()").getSettingValue()).isNull();
    }

    @Test
    void readsAConfigurationPropertiesFieldAsTheSettingItsPrefixAndNameGiveMatchedLoosely() {
        final Address replica = addressOf("com.example.addresses.OrdersReplica#open()");
        assertAll(
            () -> assertThat(replica.getValue()).isEqualTo("orders-db.jdbcUrl"),
            () -> assertThat(replica.getVia()).isEqualTo("@ConfigurationProperties"),
            () -> assertThat(replica.getSettingValue()).isEqualTo("jdbc:postgresql://orders-replica.internal/orders"));
    }

    @Test
    void flagsASettingWhoseValueIsAPlaceholder() {
        final Address warehouse = addressOf("com.example.addresses.WarehouseClient#stock()");
        assertAll(
            () -> assertThat(warehouse.getVia()).isEqualTo("@Value"),
            () -> assertThat(warehouse.getSettingValue()).isEqualTo("https://YOUR-WAREHOUSE/api"),
            () -> assertThat(warehouse.isPlaceholder()).isTrue());
    }

    @Test
    void marksConnectionsThatStayOnThisMachineAsLocal() {
        assertThat(FACTS.getConnectionPoints()).filteredOn(point -> point.getLocal() != null)
            .extracting(Facts.Point::getMethod, Facts.Point::getLocal)
            .containsExactlyInAnyOrder(
                tuple("com.example.alt.EmbeddedProvider#getConnection()", "an H2 database in memory or in a file"),
                tuple("com.example.alt.LocalThings#readBundledFile()", "a file, a jar or a classpath resource"),
                tuple("com.example.alt.LocalThings#talkToTheAgentOnThisMachine()", "this machine"),
                tuple("com.example.alt.LocalThings#readOwnJar()", "a classpath resource"),
                tuple("com.example.alt.LocalThings#openTheDatabaseOnThisMachine()", "this machine"),
                tuple("com.example.addresses.ListenAddress#toListenOn()", "an address of this machine, to listen on"),
                tuple("com.example.lookups.Peers#gateway()", "a numeric address, which nothing looks up"),
                tuple("com.example.transfers.LocalFileServer#<init>(int)", "a port of this machine, to listen on"),
                tuple("com.example.lookups.Peers#onTheLink()", "a numeric address, which nothing looks up"),
                tuple("com.example.lookups.Peers#byAddress(String)", "a numeric address, which nothing looks up"),
                tuple("com.example.lookups.TimeClient#ask(InetAddress,int)", "this machine"),
                tuple("com.example.lookups.TimeClient#ask(InetAddress,int)", "this machine"),
                tuple("com.example.pages.PageClient#get(String)", "this machine"),
                tuple("org.apache.commons.dbcp.BasicDataSource#<bean auditDataSource>()",
                    "an H2 database in memory or in a file"));
    }
}
