package com.aeg.core.fiscalizacion.mqtt;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;

@Component
public class FiscalizacionSessionRegistry {

    private final ConcurrentHashMap<String, FiscalizacionSession> byMac = new ConcurrentHashMap<>();

    public Optional<FiscalizacionSession> find(String compactMac) {
        return Optional.ofNullable(byMac.get(normalize(compactMac)));
    }

    public boolean hasActiveSession(String compactMac) {
        return find(compactMac).filter(session -> !session.isTerminal()).isPresent();
    }

    public FiscalizacionSession register(FiscalizacionSession session) {
        String key = normalize(session.compactMac());
        FiscalizacionSession existing = byMac.get(key);
        if (existing != null && !existing.isTerminal()) {
            throw new FiscalizacionProtocolException(
                    "Active fiscalizacion session already exists for MAC");
        }
        byMac.put(key, session);
        return session;
    }

    public void remove(String compactMac) {
        byMac.remove(normalize(compactMac));
    }

    public List<FiscalizacionSession> listActive() {
        return byMac.values().stream().filter(session -> !session.isTerminal()).toList();
    }

    private static String normalize(String compactMac) {
        return MacAddressNormalizer.toCompactForm(compactMac);
    }
}
