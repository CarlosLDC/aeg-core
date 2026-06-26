package com.aeg.core.enajenacion.mqtt.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MqttEnajenacionActivityLogRepository
        extends JpaRepository<MqttEnajenacionActivityLog, String>,
                JpaSpecificationExecutor<MqttEnajenacionActivityLog> {
}
