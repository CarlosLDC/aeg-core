package com.aeg.core.enajenacion.mqtt;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class EnajenacionSessionRegistry {

    private final ConcurrentHashMap<String, EnajenacionSession> byMac = new ConcurrentHashMap<>();

    public Optional<EnajenacionSession> find(String compactMac) {
        return Optional.ofNullable(byMac.get(normalize(compactMac)));
    }

    public boolean hasActiveSession(String compactMac) {
        return find(compactMac)
                .filter(session -> !session.isTerminal())
                .isPresent();
    }

    public EnajenacionSession register(EnajenacionSession session) {
        String key = normalize(session.compactMac());
        EnajenacionSession existing = byMac.get(key);
        if (existing != null && !existing.isTerminal()) {
            throw new EnajenacionProtocolException("Active enajenacion session already exists for MAC");
        }
        byMac.put(key, session);
        return session;
    }

    public void remove(String compactMac) {
        byMac.remove(normalize(compactMac));
    }

    public List<EnajenacionSession> listActive() {
        return byMac.values().stream()
                .filter(session -> !session.isTerminal())
                .toList();
    }

    private static String normalize(String compactMac) {
        return MacAddressNormalizer.toCompactForm(compactMac);
    }
}
