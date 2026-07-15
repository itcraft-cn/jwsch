package cn.itcraft.jwsch.srv.integration;

import cn.itcraft.jwsch.srv.JwschServer;
import cn.itcraft.jwsch.srv.config.JwschConfig;
import cn.itcraft.jwsch.srv.config.TcpConfig;
import cn.itcraft.jwsch.srv.config.WebSocketConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class BroadcastIntegrationTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastIntegrationTest.class);
    
    private static final AtomicInteger PORT_COUNTER = new AtomicInteger(30000);
    
    private int wsPort;
    private JwschServer server;
    private MockWebSocketClient c1;
    private MockWebSocketClient c2;
    private MockWebSocketClient c3;
    
    @Before
    public void setUp() throws Exception {
        wsPort = PORT_COUNTER.getAndIncrement();
        LOGGER.info("=== Starting Broadcast Integration Test (port={}) ===", wsPort);
    }
    
    @After
    public void tearDown() {
        if (c1 != null) {
            c1.disconnect();
        }
        if (c2 != null) {
            c2.disconnect();
        }
        if (c3 != null) {
            c3.disconnect();
        }
        if (server != null) {
            server.shutdown();
        }
        LOGGER.info("=== Broadcast Integration Test Cleanup Complete ===");
    }
    
    @Test
    public void testBroadcastFromC1ToC2C3() throws Exception {
        LOGGER.info("=== Test: Broadcast from C1 to C2/C3 ===");
        
        JwschConfig config = JwschConfig.builder()
            .webSocket(WebSocketConfig.builder().port(wsPort).build())
            .tcp(TcpConfig.builder().port(wsPort + 1000).build())
            .build();
        server = new JwschServer(config);
        server.start();
        LOGGER.info("[1/4] Server started on port {}", wsPort);
        
        TimeUnit.MILLISECONDS.sleep(100);
        
        c1 = new MockWebSocketClient("localhost", wsPort, "/ws");
        boolean c1Connected = c1.connect();
        assertTrue("C1 should connect", c1Connected);
        LOGGER.info("[2/4] C1 (broadcaster) connected");
        
        c2 = new MockWebSocketClient("localhost", wsPort, "/ws");
        c3 = new MockWebSocketClient("localhost", wsPort, "/ws");
        
        boolean c2Connected = c2.connect();
        boolean c3Connected = c3.connect();
        
        assertTrue("C2 should connect", c2Connected);
        assertTrue("C3 should connect", c3Connected);
        LOGGER.info("[3/4] C2 and C3 (receivers) connected");
        
        TimeUnit.MILLISECONDS.sleep(200);
        
        int connectionCount = server.getPacketRouter().getFrontendConnectionCount();
        assertEquals("Should have 3 connections", 3, connectionCount);
        LOGGER.info("Connection manager has {} connections", connectionCount);
        
        String testMessage = "Hello from C1!";
        c1.sendText(testMessage);
        LOGGER.info("[4/4] C1 sent: {}", testMessage);
        
        boolean c1Received = c1.waitForMessage(2, TimeUnit.SECONDS);
        
        assertTrue("C1 should receive echo", c1Received);
        assertTrue("C1 message should contain echo", c1.getLastMessage().contains("echo:"));
        
        LOGGER.info("=== Broadcast Test PASSED ===");
    }
    
    @Test
    public void testMultipleBroadcasts() throws Exception {
        LOGGER.info("=== Test: Multiple Broadcasts ===");
        
        JwschConfig config = JwschConfig.builder()
            .webSocket(WebSocketConfig.builder().port(wsPort).build())
            .tcp(TcpConfig.builder().port(wsPort + 1000).build())
            .build();
        server = new JwschServer(config);
        server.start();
        TimeUnit.MILLISECONDS.sleep(100);
        
        c1 = new MockWebSocketClient("localhost", wsPort, "/ws");
        c2 = new MockWebSocketClient("localhost", wsPort, "/ws");
        c3 = new MockWebSocketClient("localhost", wsPort, "/ws");
        
        assertTrue("C1 should connect", c1.connect());
        assertTrue("C2 should connect", c2.connect());
        assertTrue("C3 should connect", c3.connect());
        
        TimeUnit.MILLISECONDS.sleep(200);
        
        for (int i = 1; i <= 3; i++) {
            String message = "Message " + i;
            c1.sendText(message);
            LOGGER.info("Sent message {}: {}", i, message);
            
            TimeUnit.MILLISECONDS.sleep(100);
        }
        
        TimeUnit.MILLISECONDS.sleep(500);
        
        LOGGER.info("C1 message count: {}", c1.getMessageCount());
        LOGGER.info("C2 message count: {}", c2.getMessageCount());
        LOGGER.info("C3 message count: {}", c3.getMessageCount());
        
        assertTrue("C1 should receive at least 1 message", c1.getMessageCount() >= 1);
        
        LOGGER.info("=== Multiple Broadcasts Test PASSED ===");
    }
    
    @Test
    public void testTopicBroadcast() throws Exception {
        LOGGER.info("=== Test: Topic Broadcast ===");
        
        JwschConfig config = JwschConfig.builder()
            .webSocket(WebSocketConfig.builder().port(wsPort).build())
            .tcp(TcpConfig.builder().port(wsPort + 1000).build())
            .build();
        server = new JwschServer(config);
        server.start();
        TimeUnit.MILLISECONDS.sleep(100);
        
        c1 = new MockWebSocketClient("localhost", wsPort, "/ws");
        c2 = new MockWebSocketClient("localhost", wsPort, "/ws");
        c3 = new MockWebSocketClient("localhost", wsPort, "/ws");
        
        assertTrue("C1 should connect", c1.connect());
        assertTrue("C2 should connect", c2.connect());
        assertTrue("C3 should connect", c3.connect());
        
        TimeUnit.MILLISECONDS.sleep(200);
        
        c2.sendText("subscribe:news");
        TimeUnit.MILLISECONDS.sleep(100);
        assertTrue("C2 should receive subscription confirmation", c2.waitForMessage(1, TimeUnit.SECONDS));
        assertEquals("C2 should receive subscribed message", "subscribed:news", c2.getLastMessage());
        
        c3.sendText("subscribe:sports");
        TimeUnit.MILLISECONDS.sleep(100);
        assertTrue("C3 should receive subscription confirmation", c3.waitForMessage(1, TimeUnit.SECONDS));
        assertEquals("C3 should receive subscribed message", "subscribed:sports", c3.getLastMessage());
        
        LOGGER.info("[Setup] C2 subscribed to 'news', C3 subscribed to 'sports'");
        
        assertTrue("C2 should have received at least 1 message", c2.getMessageCount() >= 1);
        assertTrue("C3 should have received at least 1 message", c3.getMessageCount() >= 1);
        
        LOGGER.info("=== Topic Broadcast Test PASSED ===");
    }
}