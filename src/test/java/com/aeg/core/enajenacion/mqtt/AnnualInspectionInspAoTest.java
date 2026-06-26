package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class AnnualInspectionInspAoTest {

    @Test
    void mapsChecklistToInspAoStatuses() {
        AnnualInspectionInspAo allOk = AnnualInspectionInspAo.fromChecklist(true, true, true, true, true);
        assertThat(allOk.precinto()).isEqualTo("Bien");
        assertThat(allOk.etiqFisc()).isEqualTo("Bien");
        assertThat(allOk.impFact()).isEqualTo("Bien");
        assertThat(allOk.impNC()).isEqualTo("Bien");
        assertThat(allOk.sensPapel()).isEqualTo("Bien");

        AnnualInspectionInspAo allOff = AnnualInspectionInspAo.fromChecklist(false, false, false, false, false);
        assertThat(allOff.precinto()).isEqualTo("Violentado");
        assertThat(allOff.etiqFisc()).isEqualTo("Violentado");
        assertThat(allOff.impFact()).isEqualTo("Defectuoso");
        assertThat(allOff.impNC()).isEqualTo("Defectuoso");
        assertThat(allOff.sensPapel()).isEqualTo("Defectuoso");
    }
}
