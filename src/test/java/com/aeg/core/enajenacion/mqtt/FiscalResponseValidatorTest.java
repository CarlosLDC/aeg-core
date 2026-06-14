package com.aeg.core.enajenacion.mqtt;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.aeg.core.enajenacion.mqtt.dto.FiscalMqttResponseItem;

class FiscalResponseValidatorTest {

    private final FiscalResponseValidator validator = new FiscalResponseValidator();

    @Test
    void acceptsValidDnfResponse() {
        List<FiscalMqttResponseItem> items = IntStream.range(0, 10)
                .mapToObj(i -> new FiscalMqttResponseItem("efeNoDAnJuCeDNF", 0, 0))
                .toList();
        items = new java.util.ArrayList<>(items);
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_DNF, 0, EnajenacionConstants.DNF_END_OK));

        validator.validateDnfResponse(items);
    }

    @Test
    void rejectsDnfWithWrongEndDataD() {
        List<FiscalMqttResponseItem> items = IntStream.range(0, 10)
                .mapToObj(i -> new FiscalMqttResponseItem("efeNoDAnJuCeDNF", 0, 0))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        items.add(new FiscalMqttResponseItem(EnajenacionConstants.CMD_END_DNF, 0, 0));

        assertThatThrownBy(() -> validator.validateDnfResponse(items))
                .isInstanceOf(EnajenacionProtocolException.class);
    }
}
