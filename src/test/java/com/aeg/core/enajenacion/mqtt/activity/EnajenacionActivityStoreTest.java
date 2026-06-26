package com.aeg.core.enajenacion.mqtt.activity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aeg.core.enajenacion.mqtt.EnajenacionSessionState;

class EnajenacionActivityStoreTest {

    private EnajenacionActivityStore store;

    @BeforeEach
    void setUp() {
        store = new EnajenacionActivityStore(new InMemoryEnajenacionActivityPersistence());
    }

    @Test
    void keepsAllRecordedEntriesWithoutEviction() {
        for (int i = 0; i < 5; i++) {
            store.record(EnajenacionActivityEntry.create(
                    "MAC" + i,
                    null,
                    null,
                    EnajenacionActivityDirection.INBOUND,
                    "/topic",
                    "{\"i\":" + i + "}",
                    EnajenacionActivityResult.RECEIVED,
                    null,
                    null));
        }

        assertThat(store.find(EnajenacionActivityQuery.unrestricted(), 10, 0)).hasSize(5);
        assertThat(store.find(EnajenacionActivityQuery.unrestricted(), 10, 0).get(0).mac()).isEqualTo("MAC4");
        assertThat(store.find(EnajenacionActivityQuery.unrestricted(), 10, 0).get(4).mac()).isEqualTo("MAC0");
    }

    @Test
    void filtersByMac() {
        store.record(entryForMac("AA1122334455"));
        store.record(entryForMac("BB1122334455"));
        store.record(entryForMac("AA1122334455"));

        EnajenacionActivityQuery query = new EnajenacionActivityQuery(
                "AA:11:22:33:44:55", null, null, null, false);
        assertThat(store.find(query, 10, 0)).hasSize(2);
        assertThat(store.find(query, 10, 0)).allMatch(e -> "AA1122334455".equals(e.mac()));
    }

    @Test
    void filtersByResultAndSerial() {
        store.record(entryForMac("AA1122334455", "GRA0000001", EnajenacionActivityResult.PROCESSED));
        store.record(entryForMac("AA1122334455", "GRA0000002", EnajenacionActivityResult.FAILED));

        EnajenacionActivityQuery query = new EnajenacionActivityQuery(
                null, EnajenacionActivityResult.PROCESSED, "000001", null, false);
        assertThat(store.find(query, 10, 0)).hasSize(1);
        assertThat(store.find(query, 10, 0).get(0).ptrReg()).isEqualTo("GRA0000001");
    }

    @Test
    void respectsLimitAndPage() {
        store.record(entryForMac("AA1122334455"));
        store.record(entryForMac("AA1122334455"));
        store.record(entryForMac("AA1122334455"));

        assertThat(store.find(EnajenacionActivityQuery.unrestricted(), 2, 0)).hasSize(2);
        assertThat(store.find(EnajenacionActivityQuery.unrestricted(), 2, 1)).hasSize(1);
    }

    @Test
    void countsMatchingEntries() {
        store.record(entryForMac("AA1122334455"));
        store.record(entryForMac("BB1122334455"));

        assertThat(store.count(EnajenacionActivityQuery.unrestricted())).isEqualTo(2);
        assertThat(store.count(new EnajenacionActivityQuery(
                "AA1122334455", null, null, null, false))).isEqualTo(1);
    }

    private static EnajenacionActivityEntry entryForMac(String mac) {
        return entryForMac(mac, "GRA0000001", EnajenacionActivityResult.RECEIVED);
    }

    private static EnajenacionActivityEntry entryForMac(
            String mac,
            String ptrReg,
            EnajenacionActivityResult result) {
        return EnajenacionActivityEntry.create(
                mac,
                1L,
                ptrReg,
                EnajenacionActivityDirection.INBOUND,
                "/" + mac + "/AEG_Fiscal/Integracion/CmdServer",
                "{}",
                result,
                null,
                EnajenacionSessionState.DNF_SENT);
    }
}
