package com.aeg.core.mqtt;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Evita que la conexión MQTT bloquee el arranque de Tomcat en App Platform (health check en :8080).
 */
@Component
@ConditionalOnProperty(name = "app.mqtt.inbound.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class MqttInboundDeferredStarter {

	private final ObjectProvider<MqttPahoMessageDrivenChannelAdapter> inboundAdapter;

	public MqttInboundDeferredStarter(ObjectProvider<MqttPahoMessageDrivenChannelAdapter> inboundAdapter) {
		this.inboundAdapter = inboundAdapter;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void startInboundAfterReady() {
		MqttPahoMessageDrivenChannelAdapter adapter = inboundAdapter.getIfAvailable();
		if (adapter == null) {
			return;
		}
		Thread starter = new Thread(() -> startAdapter(adapter), "mqtt-inbound-deferred-start");
		starter.setDaemon(true);
		starter.start();
	}

	private void startAdapter(MqttPahoMessageDrivenChannelAdapter adapter) {
		try {
			if (!adapter.isRunning()) {
				adapter.start();
				log.info("MQTT inbound adapter started (deferred after application ready)");
			}
		} catch (Exception ex) {
			log.warn("MQTT inbound deferred start failed (API stays up): {}", ex.getMessage());
		}
	}
}
