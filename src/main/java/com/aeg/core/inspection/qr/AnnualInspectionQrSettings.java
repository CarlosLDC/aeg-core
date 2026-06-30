package com.aeg.core.inspection.qr;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AnnualInspectionQrSettings {

    @Value("${app.annual-inspection.qr.secret:}")
    private String secret = "";

    public String secret() {
        return secret;
    }
}
