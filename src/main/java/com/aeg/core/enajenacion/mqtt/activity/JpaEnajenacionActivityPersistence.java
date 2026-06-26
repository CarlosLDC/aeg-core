package com.aeg.core.enajenacion.mqtt.activity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.aeg.core.enajenacion.mqtt.MacAddressNormalizer;

import jakarta.persistence.criteria.Predicate;

@Component
public class JpaEnajenacionActivityPersistence implements EnajenacionActivityPersistence {

    private final MqttEnajenacionActivityLogRepository repository;

    public JpaEnajenacionActivityPersistence(MqttEnajenacionActivityLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(EnajenacionActivityEntry entry) {
        repository.save(MqttEnajenacionActivityLog.from(entry));
    }

    @Override
    public List<EnajenacionActivityEntry> find(EnajenacionActivityQuery query, int limit, int page) {
        int effectiveLimit = Math.max(1, limit);
        int effectivePage = Math.max(0, page);
        Specification<MqttEnajenacionActivityLog> spec = buildSpec(query);
        return repository
                .findAll(
                        spec,
                        PageRequest.of(
                                effectivePage,
                                effectiveLimit,
                                Sort.by(Sort.Direction.DESC, "recordedAt")))
                .stream()
                .map(MqttEnajenacionActivityLog::toEntry)
                .toList();
    }

    @Override
    public long count(EnajenacionActivityQuery query) {
        return repository.count(buildSpec(query));
    }

    @Override
    public void clear() {
        repository.deleteAll();
    }

    private static Specification<MqttEnajenacionActivityLog> buildSpec(EnajenacionActivityQuery query) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.mac() != null && !query.mac().isBlank()) {
                predicates.add(cb.equal(root.get("mac"), query.mac()));
            }
            if (query.result() != null) {
                predicates.add(cb.equal(root.get("result"), query.result()));
            }
            if (query.ptrRegContains() != null && !query.ptrRegContains().isBlank()) {
                String pattern = "%" + query.ptrRegContains().trim().toUpperCase() + "%";
                predicates.add(cb.like(cb.upper(root.get("ptrReg")), pattern));
            }
            if (query.sessionEventsOnly()) {
                predicates.add(cb.isNull(root.get("direction")));
            } else if (query.direction() != null) {
                predicates.add(cb.equal(root.get("direction"), query.direction()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    static EnajenacionActivityQuery normalizeQuery(EnajenacionActivityQuery query) {
        String mac = normalizeMacFilter(query.mac());
        String ptrReg = normalizePtrRegFilter(query.ptrRegContains());
        return new EnajenacionActivityQuery(
                mac,
                query.result(),
                ptrReg,
                query.direction(),
                query.sessionEventsOnly());
    }

    private static String normalizeMacFilter(String macFilter) {
        if (macFilter == null || macFilter.isBlank()) {
            return null;
        }
        return MacAddressNormalizer.toCompactForm(macFilter);
    }

    private static String normalizePtrRegFilter(String ptrRegFilter) {
        if (ptrRegFilter == null || ptrRegFilter.isBlank()) {
            return null;
        }
        return ptrRegFilter.trim();
    }
}
