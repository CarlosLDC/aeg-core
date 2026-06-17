package com.aeg.core.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aeg.core.enajenacion.mqtt.EnajenacionContext;
import com.aeg.core.enajenacion.mqtt.EnajenacionPreconditionValidator;
import com.aeg.core.enajenacion.mqtt.EnajenacionSession;
import com.aeg.core.enajenacion.mqtt.EnajenacionSessionRegistry;
import com.aeg.core.enajenacion.mqtt.EnajenacionSessionState;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityDirection;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityEntry;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityResult;
import com.aeg.core.enajenacion.mqtt.activity.EnajenacionActivityStore;
import com.aeg.core.mqtt.dto.EnajenacionActiveSessionResponse;

@ExtendWith(MockitoExtension.class)
class EnajenacionMqttAdminControllerTest {

    private static final String MAC = "206EF1884C68";

    @Mock
    private EnajenacionPreconditionValidator preconditionValidator;

    private EnajenacionActivityStore activityStore;
    private EnajenacionSessionRegistry sessionRegistry;
    private EnajenacionMqttAdminController controller;

    @BeforeEach
    void setUp() {
        activityStore = new EnajenacionActivityStore(50);
        sessionRegistry = new EnajenacionSessionRegistry();
        controller = new EnajenacionMqttAdminController(
                preconditionValidator, activityStore, sessionRegistry);
    }

    @Test
    void activityReturnsRecentEntries() {
        activityStore.record(EnajenacionActivityEntry.create(
                MAC,
                1L,
                "GRA0000017",
                EnajenacionActivityDirection.INBOUND,
                "/" + MAC + "/AEG_Fiscal/Integracion/CmdServer",
                "{\"cmd\":\"ptrEnajenar\"}",
                EnajenacionActivityResult.RECEIVED,
                null,
                null));

        var response = controller.activity(10, null);

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.entries().get(0).mac()).isEqualTo(MAC);
        assertThat(response.entries().get(0).result()).isEqualTo(EnajenacionActivityResult.RECEIVED);
    }

    @Test
    void sessionsReturnsActiveSessions() {
        EnajenacionContext context = new EnajenacionContext(
                "GRA0000017",
                "20:6E:F1:88:4C:68",
                1L,
                "J-12345678-9",
                "ACME",
                "CONTRIBUYENTE ORDINARIO",
                "Address",
                "Line 2",
                "Caracas, DC");
        EnajenacionSession session = new EnajenacionSession(MAC, 1L, context);
        session.setState(EnajenacionSessionState.DNF_SENT);
        sessionRegistry.register(session);

        List<EnajenacionActiveSessionResponse> sessions = controller.sessions();

        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).mac()).isEqualTo(MAC);
        assertThat(sessions.get(0).ptrReg()).isEqualTo("GRA0000017");
        assertThat(sessions.get(0).state()).isEqualTo(EnajenacionSessionState.DNF_SENT);
    }
}
