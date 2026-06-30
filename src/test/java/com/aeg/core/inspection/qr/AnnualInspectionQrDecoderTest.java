package com.aeg.core.inspection.qr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AnnualInspectionQrDecoderTest {

    private static final String EXAMPLE_QR =
            "ZSn8njvkbk7x+iu8IOFJD+OXWW65uuvLX79us586JYrENbi5Z8LiNvllg9bhB/ca";
    private static final String SECRET = "AeGsGrAsFeCh2024";

    private AnnualInspectionQrDecoder decoder;

    @BeforeEach
    void setUp() {
        AnnualInspectionQrSettings settings = new AnnualInspectionQrSettings();
        ReflectionTestUtils.setField(settings, "secret", SECRET);
        decoder = new AnnualInspectionQrDecoder(settings);
    }

    @Test
    void decodesExampleQrFromFirmwareScript() {
        AnnualInspectionQrPayload payload = decoder.decode(EXAMPLE_QR);

        assertThat(payload.registro()).isEqualTo("GRA0000017");
        assertThat(payload.mac()).isEqualTo("20:6E:F1:88:4C:68");
        assertThat(payload.fecha()).isEqualTo("29/06/2026");
    }

    @Test
    void decodesQrWithWhitespace() {
        AnnualInspectionQrPayload payload = decoder.decode(
                "  ZSn8njvkbk7x+iu8IOFJD+OXWW65uuvLX79us586JYr\nENbi5Z8LiNvllg9bhB/ca  ");

        assertThat(payload.registro()).isEqualTo("GRA0000017");
        assertThat(payload.mac()).isEqualTo("20:6E:F1:88:4C:68");
        assertThat(payload.fecha()).isEqualTo("29/06/2026");
    }

    @Test
    void decodesUrlSafeBase64() {
        AnnualInspectionQrPayload payload = decoder.decode(
                "ZSn8njvkbk7x-iu8IOFJD-OXWW65uuvLX79us586JYrENbi5Z8LiNvllg9bhB_ca");

        assertThat(payload.registro()).isEqualTo("GRA0000017");
        assertThat(payload.mac()).isEqualTo("20:6E:F1:88:4C:68");
        assertThat(payload.fecha()).isEqualTo("29/06/2026");
    }

    @Test
    void decodesQrFromUrlParameter() {
        AnnualInspectionQrPayload payload = decoder.decode(
                "https://aeg-admin.tech/verify-qr?qrCodigo=ZSn8njvkbk7x%2Biu8IOFJD%2BOXWW65uuvLX79us586JYrENbi5Z8LiNvllg9bhB%2Fca");

        assertThat(payload.registro()).isEqualTo("GRA0000017");
        assertThat(payload.mac()).isEqualTo("20:6E:F1:88:4C:68");
        assertThat(payload.fecha()).isEqualTo("29/06/2026");
    }

    @Test
    void rejectsInvalidBase64() {
        assertThatThrownBy(() -> decoder.decode("not-valid-base64!!!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(AnnualInspectionQrMessages.INVALID_CODE);
    }

    @Test
    void rejectsMissingSecret() {
        AnnualInspectionQrSettings settings = new AnnualInspectionQrSettings();
        ReflectionTestUtils.setField(settings, "secret", "");
        AnnualInspectionQrDecoder missingSecretDecoder = new AnnualInspectionQrDecoder(settings);

        assertThatThrownBy(() -> missingSecretDecoder.decode(EXAMPLE_QR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(AnnualInspectionQrMessages.INVALID_CODE);
    }
}
