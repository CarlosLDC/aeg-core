package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EnajenacionFlowStepIdsTest {

    @Test
    void mapsSentStatesToFlowStepIds() {
        assertThat(EnajenacionFlowStepIds.acceptedStepId(EnajenacionSessionState.DNF_SENT))
                .contains(EnajenacionFlowStepIds.DNF);
        assertThat(EnajenacionFlowStepIds.publishedStepId(EnajenacionSessionState.FISCAL_RIF_SENT))
                .contains(EnajenacionFlowStepIds.FISCAL_RIF);
        assertThat(EnajenacionFlowStepIds.acceptedStepId(EnajenacionSessionState.REPORT_Z_SENT))
                .contains(EnajenacionFlowStepIds.REPORT_Z);
    }

    @Test
    void ignoresNonAwaitingStates() {
        assertThat(EnajenacionFlowStepIds.acceptedStepId(EnajenacionSessionState.DNF_OK)).isEmpty();
        assertThat(EnajenacionFlowStepIds.publishedStepId(EnajenacionSessionState.COMPLETED)).isEmpty();
    }
}
