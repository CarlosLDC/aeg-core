package com.aeg.core.enajenacion.mqtt.activity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aeg.core.enajenacion.mqtt.EnajenacionSessionState;

class EnajenacionActivityStoreTest {

    private EnajenacionActivityStore store;

    @BeforeEach
    void setUp() {
        store = new EnajenacionActivityStore(3);
    }

    @Test
    void keepsMostRecentEntriesUpToCapacity() {
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

        assertThat(store.recent(10, null)).hasSize(3);
        assertThat(store.recent(10, null).get(0).mac()).isEqualTo("MAC4");
        assertThat(store.recent(10, null).get(2).mac()).isEqualTo("MAC2");
    }

    @Test
    void filtersByMac() {
        store.record(entryForMac("AA1122334455"));
        store.record(entryForMac("BB1122334455"));
        store.record(entryForMac("AA1122334455"));

        assertThat(store.recent(10, "AA:11:22:33:44:55")).hasSize(2);
        assertThat(store.recent(10, "AA1122334455")).allMatch(e -> "AA1122334455".equals(e.mac()));
    }

    @Test
    void respectsLimit() {
        store.record(entryForMac("AA1122334455"));
        store.record(entryForMac("AA1122334455"));
        store.record(entryForMac("AA1122334455"));

        assertThat(store.recent(2, null)).hasSize(2);
    }

    private static EnajenacionActivityEntry entryForMac(String mac) {
        return EnajenacionActivityEntry.create(
                mac,
                1L,
                "GRA0000001",
                EnajenacionActivityDirection.INBOUND,
                "/" + mac + "/AEG_Fiscal/Integracion/CmdServer",
                "{}",
                EnajenacionActivityResult.RECEIVED,
                null,
                EnajenacionSessionState.DNF_SENT);
    }
}
