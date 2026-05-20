# MQTT live monitor (API + aeg-core-admin)

The API subscribes to MQTT as the only broker client. Browsers connect via **WebSocket** (`/ws/mqtt`) with a JWT; they never touch the private MQTT broker.

## Backend endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/mqtt/status` | ADMIN | Inbound enabled, subscribed topic, broker URL, connection hint, last message time |
| `GET` | `/api/mqtt/subscription` | ADMIN | Active monitor topic (one at a time) |
| `PUT` | `/api/mqtt/subscription` | ADMIN | Change the single subscribed topic (`{"topic":"aeg/devices/#"}`) |
| `GET` | `/api/mqtt/messages?limit=50` | ADMIN | Recent messages from in-memory buffer |
| `WS` | `/ws/mqtt?token=<JWT>` | ADMIN (JWT query) | Live stream of inbound MQTT messages |

Wire format (WebSocket):

```json
{
  "type": "message",
  "topic": "devices/1/telemetry",
  "payload": "{\"temp\":21}",
  "receivedAt": "2025-06-01T12:00:00Z",
  "qos": 1
}
```

When the admin changes the subscription, connected clients receive:

```json
{
  "type": "subscription",
  "topic": "aeg/devices/#"
}
```

The in-memory message buffer is cleared on each topic change.

## Environment variables

See [`.env.example`](../.env.example):

- `MQTT_INBOUND_TOPIC` — wildcard pattern your devices publish to (e.g. `aeg/devices/#`)
- `MQTT_INBOUND_ENABLED` — `true` to subscribe; `false` publish-only
- `MQTT_MONITOR_BUFFER_SIZE` — default `200`

Production (DigitalOcean): set `MQTT_INBOUND_TOPIC` in App Platform to match your devices before enabling inbound.

## aeg-core-admin integration

### 1. Environment

```env
VITE_API_BASE_URL=https://your-api-host
```

### 2. Load current topic and history on mount

```typescript
const [topicInput, setTopicInput] = useState("");

const subRes = await fetch(`${API}/api/mqtt/subscription`, {
  headers: { Authorization: `Bearer ${token}` },
});
const { topic } = await subRes.json();
setTopicInput(topic);

const res = await fetch(`${API}/api/mqtt/messages?limit=50`, {
  headers: { Authorization: `Bearer ${token}` },
});
const history = await res.json();
```

### 3. Change topic (one at a time)

```typescript
async function applyTopic(topic: string) {
  const res = await fetch(`${API}/api/mqtt/subscription`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ topic }),
  });
  if (!res.ok) throw new Error(await res.text());
  const { topic: active } = await res.json();
  setTopicInput(active);
  setMessages([]); // buffer cleared server-side; reset UI too
}
```

Use a single text field + “Suscribir” button. Wildcards are allowed (`#`, `+`) if your broker supports them.

### 4. Open WebSocket

```typescript
const wsBase = API.replace(/^http/, "ws");
const ws = new WebSocket(`${wsBase}/ws/mqtt?token=${encodeURIComponent(token)}`);

ws.onmessage = (event) => {
  const msg = JSON.parse(event.data);
  if (msg.type === "message") {
    // prepend to table: msg.receivedAt, msg.topic, msg.payload
  }
  if (msg.type === "subscription") {
    setTopicInput(msg.topic);
    setMessages([]);
  }
};
```

### 5. Suggested UI

- Route: `/admin/mqtt-monitor` (visible only when `role === ADMIN`)
- Input for MQTT topic + button to call `PUT /api/mqtt/subscription` (only one active topic)
- Status badge from `GET /api/mqtt/status` (`connected`, `lastMessageAt`)
- Scrollable table: time, topic, payload (pretty-print JSON when possible)
- Optional: reuse `POST /api/mqtt/publish` on the same page for manual tests (avoid publishing to the same topic pattern you subscribe to)

### 6. Example hook

```typescript
import { useEffect, useState } from "react";

type MqttMessage = {
  type: string;
  topic: string;
  payload: string;
  receivedAt: string;
  qos?: number;
};

export function useMqttMonitor(token: string | null, apiBase: string) {
  const [messages, setMessages] = useState<MqttMessage[]>([]);
  const [status, setStatus] = useState<"connecting" | "open" | "closed">("closed");

  useEffect(() => {
    if (!token) return;

    fetch(`${apiBase}/api/mqtt/messages?limit=50`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((rows) =>
        setMessages(
          rows.map((r: { topic: string; payload: string; receivedAt: string; qos?: number }) => ({
            type: "message",
            topic: r.topic,
            payload: r.payload,
            receivedAt: r.receivedAt,
            qos: r.qos,
          }))
        )
      )
      .catch(() => undefined);

    const wsUrl =
      apiBase.replace(/^http/, "ws").replace(/\/$/, "") +
      `/ws/mqtt?token=${encodeURIComponent(token)}`;
    const ws = new WebSocket(wsUrl);
    ws.onopen = () => setStatus("open");
    ws.onclose = () => setStatus("closed");
    ws.onmessage = (ev) => {
      const msg = JSON.parse(ev.data) as MqttMessage & { type: string };
      if (msg.type === "message") {
        setMessages((prev) => [msg, ...prev].slice(0, 500));
      }
      if (msg.type === "subscription") {
        setMessages([]);
      }
    };
    return () => ws.close();
  }, [token, apiBase]);

  return { messages, status };
}
```

Use **WSS** in production (`https://` API → `wss://` WebSocket).
