package com.Backend.WebSocket;

import com.Backend.Chat.dto.Message.MessageDTO;
import com.Backend.Chat.entity.Chat.Chat;
import com.Backend.Chat.Enums.ChatType;
import com.Backend.Chat.repository.ChatRepository;
import com.Backend.Chat.service.implementation.MessageServiceImpl;
import com.Backend.User.entity.User;
import com.Backend.User.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Disabled;

@Disabled("Integration test requires full infrastructure (RabbitMQ, Redis, PostgreSQL)")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;

    private WebSocketStompClient stompClient;
    private User testUser;
    private Chat testChat;
    private String wsUrl;

    @Mock
    private MessageServiceImpl messageService;

    @BeforeEach
    void setup() {
        stompClient = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())))
        );
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        wsUrl = "ws://localhost:" + port + "/dm";

        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser = userRepository.save(testUser);

        testChat = new Chat();
        testChat.setPrivate(true);
        testChat.setChatType(ChatType.PRIVATE);
        testChat = chatRepository.save(testChat);

        when(messageService.saveMessage(any(MessageDTO.class)))
                .thenAnswer(invocation -> {
                    MessageDTO msg = invocation.getArgument(0);
                    msg.setId(999L);
                    return msg;
                });
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable);
            return http.build();
        }
    }

    @EnableWebSocketMessageBroker
    static class InMemoryBrokerConfig implements WebSocketMessageBrokerConfigurer {
        @Override
        public void configureMessageBroker(MessageBrokerRegistry config) {
            config.enableSimpleBroker("/topic", "/queue");
            config.setApplicationDestinationPrefixes("/app");
        }

        @Override
        public void registerStompEndpoints(StompEndpointRegistry registry) {
            registry.addEndpoint("/dm").withSockJS();
        }
    }

    @Test
    void testWebSocketConnection() throws Exception {
        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        assertNotNull(session);
        assertTrue(session.isConnected());
        session.disconnect();
    }

    @Test
    void testSubscription() throws Exception {
        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        StompSession.Subscription subscription = session.subscribe("/topic/chat" + testChat.getId(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
            }
        });

        assertNotNull(subscription);
        assertNotNull(subscription.getSubscriptionId());
        session.disconnect();
    }
}
