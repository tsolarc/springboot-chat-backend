package com.Backend.Chat.config.Web;

import com.Backend.Chat.Interceptor.ChatChannelInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import java.security.Principal;
import java.util.Map;

/**
 * Configuración de WebSocket + STOMP usando RabbitMQ como broker externo.
 *
 * Flujo de mensajes:
 * 1. El cliente abre conexión WebSocket (con SockJS fallback) en el endpoint "/dm".
 * 2. Para enviar un mensaje, el cliente envía un STOMP SEND a "/app/chat/{chatId}/send".
 *    - Prefijo "/app" mapea a métodos @MessageMapping en los controladores.
 * 3. El método @MessageMapping procesa y persiste el mensaje.
 * 4. Desde el controlador se hace convertAndSend a "/topic/chat.{chatId}".
 * 5. RabbitMQ recibe este destino (gracias al relay en "/topic") y reenvía
 *    el mensaje a todos los clientes suscritos.
 *
 * Suscripción de clientes:
 *   STOMP SUBSCRIBE a "/topic/chat.{chatId}" para recibir en tiempo real.
 */

@Configuration
@EnableWebSocketMessageBroker
@EnableTransactionManagement
@Profile("!test")
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.rabbitmq.host}")
    private String relayHost;

    @Value("${spring.rabbitmq.port}")
    private int RABBITMQ_PORT;

    @Value("${spring.rabbitmq.stomp-port}")
    private int RABBITMQ_STOMP_PORT;

    @Value("${spring.rabbitmq.username}")
    private String RABBITMQ_USERNAME;

    @Value("${spring.rabbitmq.password}")
    private String RABBITMQ_PASSWORD;

    @Value("${app.cors.allowed-origins:https://your-frontend-domain.com,https://www.your-frontend-domain.com,http://localhost:3000,http://localhost:5173,http://localhost:8080}")
    private String[] allowedOrigins;

    @Autowired
    private ChatChannelInterceptor chatChannelInterceptor;

    /**
     * Registra los endpoints STOMP con SockJS.
     * - "/dm": endpoint para el módulo de chat directo.
     * - "/ws-event": endpoint para eventos genéricos (si lo usas).
     */

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/dm")
                .setAllowedOriginPatterns(allowedOrigins)
                .setHandshakeHandler(handshakeHandler())
                .withSockJS();

        registry.addEndpoint("/ws-event")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }

    /**
     * Configura el broker de mensajes STOMP (RabbitMQ).
     * - setApplicationDestinationPrefixes("/app"): los envíos de cliente a servidor
     *   deben usar destinos que empiecen por "/app".
     * - enableStompBrokerRelay("/topic","/queue"): delega las rutas a RabbitMQ
     *   para publicar/suscribirse a topics y queues.
     */

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app", "/app/topic");

        config.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(relayHost)
                .setRelayPort(RABBITMQ_STOMP_PORT)
                .setClientLogin(RABBITMQ_USERNAME)
                .setClientPasscode(RABBITMQ_PASSWORD)
                .setSystemLogin(RABBITMQ_USERNAME)
                .setSystemPasscode(RABBITMQ_PASSWORD)
                .setVirtualHost("/")
                .setSystemHeartbeatSendInterval(5000)
                .setSystemHeartbeatReceiveInterval(4000)
                .setAutoStartup(true);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(chatChannelInterceptor);
    }

    private HandshakeHandler handshakeHandler() {
        return new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                return super.determineUser(request, wsHandler, attributes);
            }
        };
    }
}
