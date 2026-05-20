package com.aeg.core.mqtt;

import org.springframework.context.ApplicationEvent;

public class MqttInboundReceivedEvent extends ApplicationEvent {

	private final MqttInboundMessage message;

	public MqttInboundReceivedEvent(Object source, MqttInboundMessage message) {
		super(source);
		this.message = message;
	}

	public MqttInboundMessage message() {
		return message;
	}
}
