package com.aeg.core.fiscalizacion.mqtt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class FiscalizacionMqttConfig {

    @Bean(name = "fiscalizacionTaskScheduler")
    TaskScheduler fiscalizacionTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("fiscalizacion-mqtt-");
        scheduler.initialize();
        return scheduler;
    }
}
