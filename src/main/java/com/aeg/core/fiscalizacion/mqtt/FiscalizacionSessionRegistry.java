package com.aeg.core.fiscalizacion.mqtt;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;

@Component
public class FiscalizacionSessionRegistry {

    private static final Duration PENDING_DISTRIBUTOR_TTL = Duration.ofHours(2);

    private record PendingDistributor(Long distributorId, Instant registeredAt) {
    }

    private final ConcurrentHashMap<String, FiscalizacionSession> byMac = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PendingDistributor> pendingDistributorByPtrReg =
            new ConcurrentHashMap<>();

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

    public void registerPendingDistributor(String ptrReg, Long distributorId) {
        String key = normalizePtrReg(ptrReg);
        if (key == null) {
            return;
        }
        if (distributorId == null) {
            pendingDistributorByPtrReg.remove(key);
            return;
        }
        pendingDistributorByPtrReg.put(key, new PendingDistributor(distributorId, Instant.now()));
    }

    public void clearPendingDistributor(String ptrReg) {
        String key = normalizePtrReg(ptrReg);
        if (key != null) {
            pendingDistributorByPtrReg.remove(key);
        }
    }

    public Optional<Long> resolvePendingDistributorId(String ptrReg) {
        String key = normalizePtrReg(ptrReg);
        if (key == null) {
            return Optional.empty();
        }
        PendingDistributor pending = pendingDistributorByPtrReg.get(key);
        if (pending == null) {
            return Optional.empty();
        }
        if (Duration.between(pending.registeredAt(), Instant.now()).compareTo(PENDING_DISTRIBUTOR_TTL) > 0) {
            pendingDistributorByPtrReg.remove(key, pending);
            return Optional.empty();
        }
        return Optional.of(pending.distributorId());
    }

    private static String normalize(String compactMac) {
        return MacAddressNormalizer.toCompactForm(compactMac);
    }

    private static String normalizePtrReg(String ptrReg) {
        if (ptrReg == null || ptrReg.isBlank()) {
            return null;
        }
        return ptrReg.trim().toUpperCase(Locale.ROOT);
    }
}
