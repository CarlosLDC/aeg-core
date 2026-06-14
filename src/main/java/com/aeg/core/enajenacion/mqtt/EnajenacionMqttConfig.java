package com.aeg.core.enajenacion.mqtt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class EnajenacionMqttConfig {

    @Bean(name = "enajenacionTaskScheduler")
    TaskScheduler enajenacionTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("enajenacion-mqtt-");
        scheduler.initialize();
        return scheduler;
    }
}
