package lol.unic.gutags.network;

import lol.unic.gutags.cache.BadgeCache;
import lol.unic.gutags.protocol.Protocol;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class WsConnectionManager {
    private static final Logger LOGGER = Logger.getLogger("gutags/ws");
    private static final String WEBSOCKET_URL = "wss://gutags.unic.lol/ws";
    private static final long MAX_RECONNECT_SECONDS = 30;
    private static final long HEARTBEAT_INTERVAL_SECONDS = 30;
    private static final long HEARTBEAT_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(90);

    private final BadgeCache cache;
    private final HttpClient httpClient;
    private final AtomicBoolean running = new AtomicBoolean();
    private final Object stateLock = new Object();

    private volatile List<String> nicknames = List.of();
    private ScheduledExecutorService executor;
    private WebSocket socket;
    private ScheduledFuture<?> reconnectTask;
    private long reconnectDelaySeconds = 1;
    private long activeConnectionGeneration;
    private long lastInboundNanos;
    private volatile boolean reconnectBlocked;

    public WsConnectionManager(BadgeCache cache) {
        this.cache = cache;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        reconnectBlocked = false;
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "gutags-ws");
            thread.setDaemon(true);
            return thread;
        });
        executor.execute(this::connect);
        executor.scheduleWithFixedDelay(
                this::heartbeat,
                HEARTBEAT_INTERVAL_SECONDS,
                HEARTBEAT_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        synchronized (stateLock) {
            if (reconnectTask != null) {
                reconnectTask.cancel(false);
                reconnectTask = null;
            }
            if (socket != null) {
                socket.abort();
                socket = null;
            }
            activeConnectionGeneration++;
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public void updateNicknames(List<String> currentNicknames) {
        List<String> copy = List.copyOf(currentNicknames);
        if (copy.equals(nicknames)) {
            return;
        }
        nicknames = copy;
        WebSocket current;
        synchronized (stateLock) {
            current = socket;
        }
        if (current != null) {
            send(current, Protocol.subscribe(copy));
        }
    }

    private void connect() {
        if (!running.get() || reconnectBlocked) {
            return;
        }
        URI uri;
        try {
            uri = URI.create(WEBSOCKET_URL);
        } catch (IllegalArgumentException exception) {
            LOGGER.log(Level.WARNING, "invalid ws url; retrying", exception);
            scheduleReconnect();
            return;
        }

        long generation;
        synchronized (stateLock) {
            generation = ++activeConnectionGeneration;
        }
        WebSocket.Listener listener = new Listener(generation);
        CompletableFuture<WebSocket> future = httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(uri, listener);
        future.whenComplete((connected, failure) -> {
            if (failure != null && isCurrentGeneration(generation)) {
                LOGGER.log(Level.WARNING, "ws connection failed; retrying", failure);
                scheduleReconnect();
            }
        });
    }

    private void send(WebSocket current, String message) {
        current.sendText(message, true).exceptionally(failure -> {
            if (running.get()) {
                LOGGER.log(Level.FINE, "ws send failed", failure);
            }
            return null;
        });
    }

    private void heartbeat() {
        WebSocket current;
        boolean timedOut;
        synchronized (stateLock) {
            if (!running.get() || socket == null) {
                return;
            }
            current = socket;
            timedOut = System.nanoTime() - lastInboundNanos >= HEARTBEAT_TIMEOUT_NANOS;
            if (timedOut) {
                socket = null;
            }
        }

        if (timedOut) {
            LOGGER.warning("ws heartbeat timed out; reconnecting");
            current.abort();
            scheduleReconnect();
            return;
        }

        try {
            current.sendPing(ByteBuffer.allocate(0)).whenComplete((ignored, failure) -> {
                if (failure != null) {
                    heartbeatFailed(current, failure);
                }
            });
        } catch (RuntimeException failure) {
            heartbeatFailed(current, failure);
        }
    }

    private void heartbeatFailed(WebSocket current, Throwable failure) {
        if (!disconnected(current)) {
            return;
        }
        LOGGER.log(Level.FINE, "ws heartbeat failed; reconnecting", failure);
        current.abort();
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        if (!running.get() || reconnectBlocked || executor == null) {
            return;
        }
        synchronized (stateLock) {
            if (reconnectTask != null && !reconnectTask.isDone()) {
                return;
            }
            long delay = reconnectDelaySeconds;
            reconnectDelaySeconds = Math.min(MAX_RECONNECT_SECONDS, reconnectDelaySeconds * 2);
            reconnectTask = executor.schedule(() -> {
                synchronized (stateLock) {
                    reconnectTask = null;
                }
                connect();
            }, delay, TimeUnit.SECONDS);
        }
    }

    private final class Listener implements WebSocket.Listener {
        private final StringBuilder text = new StringBuilder();
        private final long generation;

        private Listener(long generation) {
            this.generation = generation;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            synchronized (stateLock) {
                if (!running.get() || generation != activeConnectionGeneration) {
                    webSocket.abort();
                    return;
                }
                if (socket != null && socket != webSocket) {
                    socket.abort();
                }
                lastInboundNanos = System.nanoTime();
                socket = webSocket;
                send(webSocket, Protocol.hello(nicknames));
            }
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            markInbound(webSocket);
            text.append(data);
            if (last) {
                handleMessage(webSocket, text.toString());
                text.setLength(0);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            markInbound(webSocket);
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (disconnected(webSocket)) {
                scheduleReconnect();
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            boolean shouldRetry = disconnected(webSocket);
            if (shouldRetry) {
                LOGGER.log(Level.WARNING, "ws error; retrying", error);
                scheduleReconnect();
            }
        }
    }

    private boolean isCurrentGeneration(long generation) {
        synchronized (stateLock) {
            return running.get() && !reconnectBlocked && activeConnectionGeneration == generation;
        }
    }

    private void markInbound(WebSocket source) {
        synchronized (stateLock) {
            if (socket == source) {
                lastInboundNanos = System.nanoTime();
            }
        }
    }

    private void handleMessage(WebSocket source, String json) {
        try {
            Protocol.ServerMessage message = Protocol.parseServerMessage(json);
            if (message instanceof Protocol.Snapshot snapshot) {
                synchronized (stateLock) {
                    if (socket == source) {
                        cache.applySnapshot(snapshot);
                    }
                }
            } else if (message instanceof Protocol.Update update) {
                synchronized (stateLock) {
                    if (socket == source) {
                        cache.applyUpdate(update);
                    }
                }
            } else if (message instanceof Protocol.HelloAck helloAck) {
                if (helloAck.protocolVersion() == Protocol.VERSION) {
                    synchronized (stateLock) {
                        if (socket == source) {
                            reconnectDelaySeconds = 1;
                        }
                    }
                }
            } else if (message instanceof Protocol.Error error) {
                handleServerError(source, error);
            }
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "invalid ws message ignored", exception);
        }
    }

    private void handleServerError(WebSocket source, Protocol.Error error) {
        if (!"unsupported_protocol".equals(error.code())) {
            return;
        }
        synchronized (stateLock) {
            if (socket != source) {
                return;
            }
            reconnectBlocked = true;
            socket = null;
        }
        LOGGER.warning("ws connection rejected: " + error.code());
        source.abort();
    }

    private boolean disconnected(WebSocket disconnected) {
        synchronized (stateLock) {
            if (socket != disconnected) {
                return false;
            }
            socket = null;
        }
        return !reconnectBlocked;
    }
}
