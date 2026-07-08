package com.aeg.core.mqtt.dto;

import java.util.List;

public record ToolsWifiScanResponse(boolean success, String message, List<ToolsWifiNetworkDto> networks) {

    public static ToolsWifiScanResponse ok(List<ToolsWifiNetworkDto> networks) {
        return new ToolsWifiScanResponse(true, null, networks);
    }
}
