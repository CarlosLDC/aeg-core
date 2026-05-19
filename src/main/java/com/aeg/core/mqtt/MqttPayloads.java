package com.aeg.core.mqtt;

import java.util.Collection;
import java.util.Map;

final class MqttPayloads {

    private MqttPayloads() {
    }

    static boolean isEmpty(Object payload) {
        if (payload == null) {
            return true;
        }
        if (payload instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        if (payload instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return true;
    }
}
