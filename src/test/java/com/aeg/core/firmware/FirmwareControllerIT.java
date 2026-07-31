package com.aeg.core.firmware;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.aeg.core.firmware.storage.FirmwareStorage;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"app.firmware.public-base-url=http://206.189.231.128/downloads",
				"spring.jpa.hibernate.ddl-auto=update"
		})
class FirmwareControllerIT {

	@LocalServerPort
	int port;

	@MockitoBean
	FirmwareStorage firmwareStorage;

	@Test
	void uploadListGetDownloadAndDelete() throws Exception {
		byte[] payload = ("fw-" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
		String fileName = "it-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + ".bin";

		doNothing().when(firmwareStorage).upload(anyString(), any(), anyLong());
		when(firmwareStorage.download(eq(fileName))).thenReturn(payload);
		doNothing().when(firmwareStorage).delete(eq(fileName));

		String boundary = "----FirmwareBoundary" + UUID.randomUUID();
		byte[] multipart = buildMultipart(boundary, fileName, payload, "9.9.9", "integration test");

		HttpClient client = HttpClient.newHttpClient();
		String auth = basicAuthHeader();

		HttpResponse<String> createRes = client.send(
				HttpRequest.newBuilder()
						.uri(URI.create("http://localhost:" + port + "/api/firmwares"))
						.header("Authorization", auth)
						.header("Content-Type", "multipart/form-data; boundary=" + boundary)
						.POST(HttpRequest.BodyPublishers.ofByteArray(multipart))
						.build(),
				HttpResponse.BodyHandlers.ofString());

		assertThat(createRes.statusCode()).isEqualTo(201);
		assertThat(createRes.body()).contains(fileName);
		assertThat(createRes.body()).contains("http://206.189.231.128/downloads/" + fileName);
		assertThat(createRes.body()).contains("\"version\":\"9.9.9\"");

		Long id = extractId(createRes.body());

		HttpResponse<String> listRes = client.send(
				HttpRequest.newBuilder()
						.uri(URI.create("http://localhost:" + port + "/api/firmwares"))
						.header("Authorization", auth)
						.GET()
						.build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(listRes.statusCode()).isEqualTo(200);
		assertThat(listRes.body()).contains(fileName);

		HttpResponse<String> getRes = client.send(
				HttpRequest.newBuilder()
						.uri(URI.create("http://localhost:" + port + "/api/firmwares/" + id))
						.header("Authorization", auth)
						.GET()
						.build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(getRes.statusCode()).isEqualTo(200);
		assertThat(getRes.body()).contains("\"id\":" + id);

		HttpResponse<byte[]> downloadRes = client.send(
				HttpRequest.newBuilder()
						.uri(URI.create("http://localhost:" + port + "/api/firmwares/" + id + "/download"))
						.header("Authorization", auth)
						.GET()
						.build(),
				HttpResponse.BodyHandlers.ofByteArray());
		assertThat(downloadRes.statusCode()).isEqualTo(200);
		assertThat(downloadRes.headers().firstValue("Content-Type").orElse(""))
				.contains(MediaType.APPLICATION_OCTET_STREAM_VALUE);
		assertThat(downloadRes.body()).isEqualTo(payload);

		HttpResponse<String> deleteRes = client.send(
				HttpRequest.newBuilder()
						.uri(URI.create("http://localhost:" + port + "/api/firmwares/" + id))
						.header("Authorization", auth)
						.DELETE()
						.build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(deleteRes.statusCode()).isEqualTo(204);
		verify(firmwareStorage).delete(fileName);
	}

	private static String basicAuthHeader() {
		String token = Base64.getEncoder()
				.encodeToString("admin@test.local:test-admin-password".getBytes(StandardCharsets.UTF_8));
		return "Basic " + token;
	}

	private static Long extractId(String json) {
		String marker = "\"id\":";
		int idx = json.indexOf(marker);
		assertThat(idx).isGreaterThanOrEqualTo(0);
		int start = idx + marker.length();
		int end = start;
		while (end < json.length() && Character.isDigit(json.charAt(end))) {
			end++;
		}
		return Long.valueOf(json.substring(start, end));
	}

	private static byte[] buildMultipart(
			String boundary,
			String fileName,
			byte[] fileBytes,
			String version,
			String notes) {
		String preamble = "--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
				+ "Content-Type: application/octet-stream\r\n\r\n";
		String mid = "\r\n--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"version\"\r\n\r\n"
				+ version + "\r\n"
				+ "--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"notes\"\r\n\r\n"
				+ notes + "\r\n"
				+ "--" + boundary + "--\r\n";
		byte[] head = preamble.getBytes(StandardCharsets.UTF_8);
		byte[] tail = mid.getBytes(StandardCharsets.UTF_8);
		byte[] all = new byte[head.length + fileBytes.length + tail.length];
		System.arraycopy(head, 0, all, 0, head.length);
		System.arraycopy(fileBytes, 0, all, head.length, fileBytes.length);
		System.arraycopy(tail, 0, all, head.length + fileBytes.length, tail.length);
		return all;
	}
}
