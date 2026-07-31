package com.aeg.core.firmware;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FirmwareFileNamesTest {

	@Test
	void sanitizeAcceptsSafeBinName() {
		assertThat(FirmwareFileNames.sanitize("fw_1.2.3.bin")).isEqualTo("fw_1.2.3.bin");
	}

	@Test
	void sanitizeStripsPathAndRejectsUnsafeChars() {
		assertThat(FirmwareFileNames.sanitize("C:\\uploads\\aeg-fw.bin")).isEqualTo("aeg-fw.bin");
		assertThatThrownBy(() -> FirmwareFileNames.sanitize("bad name.bin"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("letters");
		assertThatThrownBy(() -> FirmwareFileNames.sanitize("firmware.hex"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(".bin");
	}

	@Test
	void buildPublicUrlJoinsWithoutDoubleSlash() {
		assertThat(FirmwareFileNames.buildPublicUrl("http://206.189.231.128/downloads/", "a.bin"))
				.isEqualTo("http://206.189.231.128/downloads/a.bin");
		assertThat(FirmwareFileNames.buildPublicUrl("http://206.189.231.128/downloads", "a.bin"))
				.isEqualTo("http://206.189.231.128/downloads/a.bin");
	}
}
