package com.aeg.core.enajenacion.mqtt.activity;

import java.util.List;

public interface EnajenacionActivityPersistence {

    void save(EnajenacionActivityEntry entry);

    List<EnajenacionActivityEntry> find(EnajenacionActivityQuery query, int limit, int page);

    long count(EnajenacionActivityQuery query);

    void clear();
}
