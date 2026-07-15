package cn.itcraft.jwsch.srv.metrics;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ServerMetricsTest {
    
    private DefaultServerMetrics serverMetrics;
    
    @Before
    public void setUp() {
        serverMetrics = new DefaultServerMetrics();
    }
    
    @Test
    public void testWebSocketConnections() {
        assertEquals(0, serverMetrics.getWebSocketConnections());
        
        serverMetrics.incrementWebSocketConnections();
        assertEquals(1, serverMetrics.getWebSocketConnections());
        
        serverMetrics.incrementWebSocketConnections();
        assertEquals(2, serverMetrics.getWebSocketConnections());
        
        serverMetrics.decrementWebSocketConnections();
        assertEquals(1, serverMetrics.getWebSocketConnections());
    }
    
    @Test
    public void testTcpConnections() {
        assertEquals(0, serverMetrics.getTcpConnections());
        
        serverMetrics.incrementTcpConnections();
        assertEquals(1, serverMetrics.getTcpConnections());
        
        serverMetrics.incrementTcpConnections();
        assertEquals(2, serverMetrics.getTcpConnections());
        
        serverMetrics.decrementTcpConnections();
        assertEquals(1, serverMetrics.getTcpConnections());
    }
    
    @Test
    public void testRecordPacketReceived() {
        serverMetrics.recordPacketReceived(100);
        serverMetrics.recordPacketReceived(200);
        
        assertNotNull(serverMetrics.scrapePrometheus());
    }
    
    @Test
    public void testRecordPacketSent() {
        serverMetrics.recordPacketSent(100);
        serverMetrics.recordPacketSent(200);
        
        assertNotNull(serverMetrics.scrapePrometheus());
    }
    
    @Test
    public void testRecordPacketDropped() {
        serverMetrics.recordPacketDropped();
        serverMetrics.recordPacketDropped();
        
        assertNotNull(serverMetrics.scrapePrometheus());
    }
    
    @Test
    public void testRecordProcessTime() {
        serverMetrics.recordProcessTime(100, TimeUnit.MILLISECONDS);
        serverMetrics.recordProcessTime(200, TimeUnit.MILLISECONDS);
        
        assertNotNull(serverMetrics.scrapePrometheus());
    }
    
    @Test
    public void testRecordRouteTime() {
        serverMetrics.recordRouteTime(50, TimeUnit.MILLISECONDS);
        serverMetrics.recordRouteTime(150, TimeUnit.MILLISECONDS);
        
        assertNotNull(serverMetrics.scrapePrometheus());
    }
    
    @Test
    public void testScrapePrometheus() {
        serverMetrics.incrementWebSocketConnections();
        serverMetrics.incrementTcpConnections();
        serverMetrics.recordPacketReceived(100);
        
        String result = serverMetrics.scrapePrometheus();
        
        assertNotNull(result);
        assertTrue(result.contains("jwsch_websocket_connections"));
        assertTrue(result.contains("jwsch_tcp_connections"));
    }
}