package com.Backend.integration;

import com.Backend.BackendApplication;
import com.Backend.Chat.entity.Message.MessageOutboxEvent;
import com.Backend.Chat.outbox.OutboxProperties;
import com.Backend.Chat.outbox.OutboxStore;
import com.Backend.Chat.outbox.RealtimeBrokerHealth;
import com.Backend.Chat.repository.MessageOutboxRepository;
import com.Backend.Chat.service.implementation.ChatServiceImpl;
import com.Backend.Chat.service.implementation.OutboxScheduledTask;
import com.Backend.Chat.service.implementation.ScheduledTaskAuditService;
import com.Backend.Security.Service.JWTService;
import com.Backend.User.entity.User;
import com.Backend.User.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/** Runs only with -Pintegration verify. All state belongs to disposable containers. */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
class MessagingProductionIT {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> rabbit = new GenericContainer<>("rabbitmq:4-management")
            .withEnv("RABBITMQ_DEFAULT_USER", "chat")
            .withEnv("RABBITMQ_DEFAULT_PASS", "integration-test")
            .withExposedPorts(5672, 61613)
            .withCommand("sh", "-c", "rabbitmq-plugins enable --offline rabbitmq_stomp && rabbitmq-server")
            .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry properties) {
        connectionProperties().forEach((key, value) -> properties.add(key, () -> value));
    }

    private static Map<String, String> connectionProperties() {
        return Map.ofEntries(
                Map.entry("spring.datasource.url", postgres.getJdbcUrl()),
                Map.entry("spring.datasource.username", postgres.getUsername()),
                Map.entry("spring.datasource.password", postgres.getPassword()),
                Map.entry("spring.rabbitmq.host", rabbit.getHost()),
                Map.entry("spring.rabbitmq.port", rabbit.getMappedPort(5672).toString()),
                Map.entry("spring.rabbitmq.stomp-port", rabbit.getMappedPort(61613).toString()),
                Map.entry("spring.rabbitmq.username", "chat"),
                Map.entry("spring.rabbitmq.password", "integration-test"),
                Map.entry("spring.data.redis.host", redis.getHost()),
                Map.entry("spring.data.redis.port", redis.getMappedPort(6379).toString()),
                Map.entry("spring.data.redis.password", ""));
    }

    @Autowired MessageOutboxRepository events;
    @Autowired OutboxStore store;
    @Autowired OutboxProperties settings;
    @Autowired ScheduledTaskAuditService audit;
    @Autowired OutboxScheduledTask worker;
    @Autowired JdbcTemplate jdbc;
    @Autowired RabbitTemplate amqp;
    @Autowired RabbitAdmin rabbitAdmin;
    @Autowired RabbitListenerEndpointRegistry listeners;
    @Autowired RealtimeBrokerHealth relay;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ConfigurableApplicationContext application;
    @Autowired UserRepository users;
    @Autowired JWTService jwt;
    @Autowired ChatServiceImpl chats;
    @LocalServerPort int port;

    @BeforeEach
    void clearOutbox() {
        listeners.stop();
        jdbc.update("DELETE FROM scheduled_task_event_log");
        jdbc.update("DELETE FROM scheduled_task_log");
        jdbc.update("DELETE FROM message_outbox");
        jdbc.update("DELETE FROM scheduled_tasks");
        settings.setMaxAttempts(12);
        settings.setBatchSize(50);
        rabbitAdmin.initialize();
        rabbitAdmin.purgeQueue("chat.realtime.queue", false);
        rabbitAdmin.purgeQueue("chat.dead.queue", false);
    }

    @Test
    void migrationUpgradesExistingV4OutboxWithoutLosingEvents() {
        String schema = "outbox_upgrade";
        var oldVersion = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(schema).defaultSchema(schema).target("4").load();
        var currentVersion = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(schema).defaultSchema(schema).cleanDisabled(false).load();
        try {
            oldVersion.migrate();
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO outbox_upgrade.message_outbox (id, routing_key, payload, created_at, next_attempt_at) "
                    + "VALUES (?, 'message.created', '{}', now(), now())", id);
            currentVersion.migrate();
            currentVersion.validate();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_upgrade.message_outbox WHERE id = ? "
                    + "AND claim_token IS NULL AND dead_lettered_at IS NULL", Integer.class, id)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE schemaname = ? "
                    + "AND indexname = 'idx_message_outbox_pending'", Integer.class, schema)).isEqualTo(1);
        } finally {
            currentVersion.clean();
        }
    }

    @Test
    void concurrentWorkerSkipsAnEventLockedByAnotherTransaction() throws Exception {
        var first = saveEvent("message.created");
        var second = saveEvent("message.created");
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var owner = executor.submit(() -> new TransactionTemplate(transactions).execute(transaction -> {
                var claim = store.claimNext().orElseThrow();
                locked.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test did not release row lock");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
                return claim;
            }));
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            var concurrent = executor.submit(() -> store.claimNext().orElseThrow()).get(3, TimeUnit.SECONDS);
            release.countDown();
            var initial = owner.get(5, TimeUnit.SECONDS);
            assertThat(List.of(initial.getId(), concurrent.getId()))
                    .containsExactlyInAnyOrder(first.getId(), second.getId());
            assertThat(initial.getClaimToken()).isNotEqualTo(concurrent.getClaimToken());
            assertThat(store.claimNext()).isEmpty();
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void expiredClaimCanBeRecoveredAndOldWorkerCannotOverwriteIt() {
        saveEvent("message.created");
        var oldClaim = store.claimNext().orElseThrow();
        jdbc.update("UPDATE message_outbox SET claimed_until = now() - interval '1 second'");
        var newClaim = store.claimNext().orElseThrow();
        assertThat(newClaim.getAttempts()).isEqualTo(2);
        assertThat(newClaim.getClaimToken()).isNotEqualTo(oldClaim.getClaimToken());
        UUID run = startRun();
        assertThat(store.settle(run, oldClaim, null, 1)).isFalse();
        assertThat(store.settle(run, newClaim, null, 1)).isTrue();
        assertThat(events.findById(newClaim.getId()).orElseThrow().getPublishedAt()).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM scheduled_task_event_log", Integer.class)).isEqualTo(1);
    }

    @Test
    void retryLimitQuarantinesTheEventAndRecordsItsRequestId() {
        settings.setMaxAttempts(2);
        saveEvent("message.created");
        UUID run = startRun();
        var first = store.claimNext().orElseThrow();
        assertThat(store.settle(run, first, new IllegalStateException("offline"), 2)).isTrue();
        var pending = events.findById(first.getId()).orElseThrow();
        assertThat(pending.getNextAttemptAt()).isAfter(Instant.now());
        assertThat(pending.getDeadLetteredAt()).isNull();
        assertThat(store.claimNext()).isEmpty();
        jdbc.update("UPDATE message_outbox SET next_attempt_at = now() - interval '1 second'");
        assertThat(store.settle(run, store.claimNext().orElseThrow(), new IllegalStateException("offline"), 2)).isTrue();
        assertThat(events.findById(first.getId()).orElseThrow().getDeadLetteredAt()).isNotNull();
        assertThat(store.claimNext()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT origin_request_id FROM scheduled_task_event_log "
                + "WHERE status = 'DEAD_LETTERED'", String.class)).isEqualTo("integration-request");
    }

    @Test
    void auditFailureRollsBackPublicationState() {
        saveEvent("message.created");
        var claim = store.claimNext().orElseThrow();
        assertThatThrownBy(() -> store.settle(UUID.randomUUID(), claim, null, 1))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        var persisted = events.findById(claim.getId()).orElseThrow();
        assertThat(persisted.getPublishedAt()).isNull();
        assertThat(persisted.getClaimToken()).isEqualTo(claim.getClaimToken());
        assertThat(store.settle(startRun(), claim, null, 1)).isTrue();
    }

    @Test
    void unroutablePublicationIsRetriedInsteadOfMarkedPublished() {
        var event = saveEvent("unknown.destination");
        worker.run();
        var persisted = events.findById(event.getId()).orElseThrow();
        assertThat(persisted.getPublishedAt()).isNull();
        assertThat(persisted.getAttempts()).isEqualTo(1);
        assertThat(persisted.getClaimToken()).isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM scheduled_task_event_log", String.class)).isEqualTo("FAILED");
    }

    @Test
    void pendingEventIsPublishedAfterRabbitMqRecovers() throws Exception {
        var event = saveEvent("message.created");
        try {
            assertThat(rabbit.execInContainer("rabbitmqctl", "stop_app").getExitCode()).isZero();
            worker.run();
            assertThat(events.findById(event.getId()).orElseThrow().getPublishedAt()).isNull();
        } finally {
            assertThat(rabbit.execInContainer("rabbitmqctl", "start_app").getExitCode()).isZero();
        }
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(amqp.<Boolean>execute(channel -> channel.isOpen())).isTrue());
        rabbitAdmin.initialize();
        jdbc.update("UPDATE message_outbox SET next_attempt_at = now() - interval '1 second'");
        worker.run();
        var delivered = amqp.receive("chat.realtime.queue", 5000);
        assertThat(delivered).isNotNull();
        assertThat(delivered.getMessageProperties().getMessageId()).isEqualTo(event.getId().toString());
        assertThat(delivered.getMessageProperties().getHeader("requestId").toString()).isEqualTo("integration-request");
        assertThat(events.findById(event.getId()).orElseThrow().getPublishedAt()).isNotNull();
        assertThat(jdbc.queryForList("SELECT status FROM scheduled_task_event_log ORDER BY id", String.class))
                .containsExactly("FAILED", "SUCCESS");
    }

    @Test
    void invalidEnvelopeReachesDeadLetterQueue() {
        await().atMost(Duration.ofSeconds(30)).until(relay::isAvailable);
        listeners.start();
        try {
            amqp.send("messaging.events", "message.created",
                    new Message("{\"unexpected\":true}".getBytes(StandardCharsets.UTF_8), new MessageProperties()));
            var dead = amqp.receive("chat.dead.queue", 30000);
            assertThat(dead).isNotNull();
            assertThat(dead.getMessageProperties().getXDeathHeader()).isNotEmpty();
        } finally {
            listeners.stop();
        }
    }

    @Test
    void twoInstancesDeliverToBothClientsAndHttpRetryDoesNotDuplicateMessage() throws Exception {
        var arguments = new java.util.ArrayList<String>();
        connectionProperties().forEach((key, value) -> arguments.add("--" + key + "=" + value));
        arguments.add("--server.port=0");
        try (var second = new SpringApplicationBuilder(BackendApplication.class).profiles("integration")
                .registerShutdownHook(false).run(arguments.toArray(String[]::new))) {
            await().atMost(Duration.ofSeconds(30)).until(() -> relay.isAvailable()
                    && second.getBean(RealtimeBrokerHealth.class).isAvailable());
            var alice = users.save(User.builder().username("alice-" + UUID.randomUUID()).build());
            var bob = users.save(User.builder().username("bob-" + UUID.randomUUID()).build());
            Long chatId = chats.getOrCreateDirectChat(alice.getId(), bob.getId()).getId();
            int secondPort = ((ServletWebServerApplicationContext) second).getWebServer().getPort();
            listeners.start();
            second.getBean(RabbitListenerEndpointRegistry.class).start();
            try (var firstClient = new Subscriber(port, jwt.generateToken(alice), chatId);
                 var secondClient = new Subscriber(secondPort, jwt.generateToken(bob), chatId)) {
                var schedulerOne = scheduler(application, "integration-one");
                var schedulerTwo = scheduler(second, "integration-two");
                try {
                    schedulerOne.start();
                    schedulerTwo.start();
                    var headers = new HttpHeaders();
                    headers.setBearerAuth(jwt.generateToken(alice));
                    headers.add("X-Request-Id", "two-instance-request");
                    UUID clientId = UUID.randomUUID();
                    var command = new HttpEntity<>(Map.of("clientMessageId", clientId, "content", "hello from instance one"), headers);
                    var rest = new TestRestTemplate();
                    String path = "/v1/conversations/" + chatId + "/messages";
                    var created = rest.postForEntity("http://localhost:" + port + path, command, JsonNode.class);
                    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                    var retried = rest.postForEntity("http://localhost:" + secondPort + path, command, JsonNode.class);
                    assertThat(retried.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(retried.getBody().get("id")).isEqualTo(created.getBody().get("id"));
                    var firstEvent = firstClient.messages.poll(15, TimeUnit.SECONDS);
                    var secondEvent = secondClient.messages.poll(15, TimeUnit.SECONDS);
                    assertThat(firstEvent).isNotNull();
                    assertThat(secondEvent).isNotNull();
                    assertThat(secondEvent.get("eventId")).isEqualTo(firstEvent.get("eventId"));
                    assertThat(firstEvent.get("clientMessageId").asText()).isEqualTo(clientId.toString());
                    await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                            assertThat(jdbc.queryForObject("SELECT count(*) FROM message_outbox WHERE published_at IS NOT NULL", Integer.class))
                                    .isEqualTo(1));
                    assertThat(jdbc.queryForObject("SELECT count(*) FROM messages WHERE client_message_id = ?", Integer.class, clientId)).isEqualTo(1);
                    assertThat(jdbc.queryForObject("SELECT origin_request_id FROM scheduled_task_event_log WHERE status = 'SUCCESS'", String.class))
                            .isEqualTo("two-instance-request");
                    assertThat(jdbc.queryForObject("SELECT count(*) FROM scheduled_tasks", Integer.class)).isEqualTo(settings.getWorkers());
                } finally {
                    schedulerOne.stop();
                    schedulerTwo.stop();
                }
            } finally {
                listeners.stop();
            }
        }
    }

    private Scheduler scheduler(ConfigurableApplicationContext context, String name) {
        var task = context.getBean("publishMessageOutbox", RecurringTask.class);
        return Scheduler.create(context.getBean(DataSource.class)).startTasks(task)
                .schedulerName(() -> name).threads(2).pollingInterval(Duration.ofMillis(200)).build();
    }

    private UUID startRun() {
        UUID run = UUID.randomUUID();
        audit.start(run, Instant.now());
        return run;
    }

    private MessageOutboxEvent saveEvent(String routingKey) {
        var event = new MessageOutboxEvent();
        event.setId(UUID.randomUUID());
        event.setRoutingKey(routingKey);
        event.setPayload("{}");
        event.setCreatedAt(Instant.now().minusSeconds(1));
        event.setNextAttemptAt(event.getCreatedAt());
        event.setOriginRequestId("integration-request");
        return events.saveAndFlush(event);
    }

    private static class Subscriber implements AutoCloseable {
        final BlockingQueue<JsonNode> messages = new LinkedBlockingQueue<>();
        final ThreadPoolTaskScheduler timers = new ThreadPoolTaskScheduler();
        final WebSocketStompClient client = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        StompSession session;

        Subscriber(int port, String token, Long chatId) throws Exception {
            timers.initialize();
            client.setTaskScheduler(timers);
            client.setMessageConverter(new MappingJackson2MessageConverter());
            var headers = new StompHeaders();
            headers.add("Authorization", "Bearer " + token);
            try {
                session = client.connectAsync("http://localhost:" + port + "/dm", new WebSocketHttpHeaders(),
                        headers, new StompSessionHandlerAdapter() {}).get(15, TimeUnit.SECONDS);
                session.setAutoReceipt(true);
                var ready = new CountDownLatch(1);
                session.subscribe("/topic/chat." + chatId, new StompFrameHandler() {
                    @Override public Type getPayloadType(StompHeaders response) { return JsonNode.class; }
                    @Override public void handleFrame(StompHeaders response, Object payload) { messages.add((JsonNode) payload); }
                }).addReceiptTask(ready::countDown);
                assertThat(ready.await(10, TimeUnit.SECONDS)).as("Broker acknowledged subscription").isTrue();
            } catch (Exception | AssertionError failure) {
                close();
                throw failure;
            }
        }

        @Override
        public void close() {
            if (session != null && session.isConnected()) session.disconnect();
            client.stop();
            timers.shutdown();
        }
    }
}
