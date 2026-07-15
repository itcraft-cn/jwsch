package cn.itcraft.jwsch.srv.integration;

import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketHeader;
import cn.itcraft.jwsch.srv.JwschServer;
import cn.itcraft.jwsch.srv.config.JwschConfig;
import cn.itcraft.jwsch.srv.config.TcpConfig;
import cn.itcraft.jwsch.srv.config.WebSocketConfig;
import cn.itcraft.jwsch.srv.registry.ServiceInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class IntegrationTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);
    
    private static final AtomicInteger PORT_COUNTER = new AtomicInteger(20000);
    
    private int wsPort;
    private int tcpBackendPort;
    
    private JwschServer server;
    private MockTcpServer tcpBackend;
    private MockWebSocketClient wsClient1;
    private MockWebSocketClient wsClient2;
    
    @Before
    public void setUp() throws Exception {
        wsPort = PORT_COUNTER.getAndIncrement();
        tcpBackendPort = PORT_COUNTER.getAndIncrement();
        LOGGER.info("=== Starting Integration Test (WS={}, TCP={}) ===", wsPort, tcpBackendPort);
    }
    
    @After
    public void tearDown() {
        if (wsClient1 != null) {
            wsClient1.disconnect();
        }
        if (wsClient2 != null) {
            wsClient2.disconnect();
        }
        if (tcpBackend != null) {
            tcpBackend.stop();
        }
        if (server != null) {
            server.shutdown();
        }
        LOGGER.info("=== Integration Test Cleanup Complete ===");
    }
    
    @Test
    public void testServerStartup() throws Exception {
        LOGGER.info("--- Test: Server Startup ---");
        
        JwschConfig config = JwschConfig.builder()
            .webSocket(WebSocketConfig.builder().port(wsPort).build())
            .tcp(TcpConfig.builder().port(tcpBackendPort).build())
            .build();
        
        server = new JwschServer(config);
        server.start();
        
        assertTrue("Server should be started", server.isStarted());
        assertNotNull("ServiceRegistry should not be null", server.getServiceRegistry());
        assertNotNull("LoadBalance should not be null", server.getLoadBalance());
        
        LOGGER.info("Server started successfully on port {}", wsPort);
    }
    
    @Test
    public void testWebSocketClientConnection() throws Exception {
        LOGGER.info("--- Test: WebSocket Client Connection ---");
        
        startServer();
        
        wsClient1 = new MockWebSocketClient("localhost", wsPort, "/ws");
        boolean connected = wsClient1.connect();
        
        assertTrue("WebSocket client should connect", connected);
        assertTrue("WebSocket client should be connected", wsClient1.isConnected());
        
        LOGGER.info("WebSocket client connected successfully");
        
        wsClient1.disconnect();
        assertFalse("WebSocket client should be disconnected", wsClient1.isConnected());
    }
    
    @Test
    public void testMultipleWebSocketClients() throws Exception {
        LOGGER.info("--- Test: Multiple WebSocket Clients ---");
        
        startServer();
        
        wsClient1 = new MockWebSocketClient("localhost", wsPort, "/ws");
        wsClient2 = new MockWebSocketClient("localhost", wsPort, "/ws");
        
        boolean connected1 = wsClient1.connect();
        boolean connected2 = wsClient2.connect();
        
        assertTrue("WebSocket client 1 should connect", connected1);
        assertTrue("WebSocket client 2 should connect", connected2);
        
        assertTrue("WebSocket client 1 should be connected", wsClient1.isConnected());
        assertTrue("WebSocket client 2 should be connected", wsClient2.isConnected());
        
        LOGGER.info("Both WebSocket clients connected successfully");
    }
    
    @Test
    public void testTcpBackendServer() throws Exception {
        LOGGER.info("--- Test: TCP Backend Server ---");
        
        tcpBackend = new MockTcpServer(tcpBackendPort);
        tcpBackend.start();
        
        assertTrue("TCP backend should be started", tcpBackend.isStarted());
        assertEquals("TCP backend port should match", tcpBackendPort, tcpBackend.getPort());
        
        LOGGER.info("TCP backend server started on port {}", tcpBackendPort);
    }
    
    @Test
    public void testServiceRegistration() throws Exception {
        LOGGER.info("--- Test: Service Registration ---");
        
        startServer();
        
        ServiceInstance instance1 = new ServiceInstance("backend-service", "localhost", tcpBackendPort);
        ServiceInstance instance2 = new ServiceInstance("backend-service", "localhost", tcpBackendPort + 1);
        
        server.getServiceRegistry().register(instance1);
        server.getServiceRegistry().register(instance2);
        
        assertEquals("Should have 2 instances", 2, 
            server.getServiceRegistry().getInstances("backend-service").size());
        
        LOGGER.info("Service instances registered: {}", 
            server.getServiceRegistry().getInstances("backend-service"));
    }
    
    @Test
    public void testFullIntegration() throws Exception {
        LOGGER.info("=== Full Integration Test ===");
        LOGGER.info("Scenario: WS clients -> Server -> TCP backend");
        
        startServer();
        LOGGER.info("[1/5] Server started on port {}", wsPort);
        
        tcpBackend = new MockTcpServer(tcpBackendPort);
        tcpBackend.start();
        LOGGER.info("[2/5] TCP backend started on port {}", tcpBackendPort);
        
        ServiceInstance backend = new ServiceInstance("backend-service", "localhost", tcpBackendPort);
        server.getServiceRegistry().register(backend);
        LOGGER.info("[3/5] Backend service registered");
        
        wsClient1 = new MockWebSocketClient("localhost", wsPort, "/ws");
        wsClient2 = new MockWebSocketClient("localhost", wsPort, "/ws");
        
        boolean c1 = wsClient1.connect();
        boolean c2 = wsClient2.connect();
        
        assertTrue("WS client 1 should connect", c1);
        assertTrue("WS client 2 should connect", c2);
        LOGGER.info("[4/5] WebSocket clients connected: c1={}, c2={}", c1, c2);
        
        wsClient1.sendText("Hello from client 1");
        wsClient2.sendText("Hello from client 2");
        LOGGER.info("[5/5] Test messages sent");
        
        TimeUnit.MILLISECONDS.sleep(500);
        
        LOGGER.info("=== Full Integration Test PASSED ===");
    }
    
    private void startServer() {
        int tcpPort = wsPort + 100;
        JwschConfig config = JwschConfig.builder()
            .webSocket(WebSocketConfig.builder().port(wsPort).build())
            .tcp(TcpConfig.builder().port(tcpPort).build())
            .build();
        
        server = new JwschServer(config);
        server.start();
    }
}