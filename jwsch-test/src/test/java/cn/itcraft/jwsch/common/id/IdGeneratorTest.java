package cn.itcraft.jwsch.common.id;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class IdGeneratorTest {
    
    private IdGenerator idGenerator;
    
    @Before
    public void setUp() {
        idGenerator = new IdGenerator();
    }
    
    @Test
    public void testGenerateId_normalCase() {
        long id = idGenerator.generateId("test");
        assertTrue(id != 0);
    }
    
    @Test
    public void testGenerateId_sameInput_sameOutput() {
        String input = "test-input";
        long id1 = idGenerator.generateId(input);
        long id2 = idGenerator.generateId(input);
        assertEquals(id1, id2);
    }
    
    @Test
    public void testGenerateId_differentInput_differentOutput() {
        long id1 = idGenerator.generateId("input1");
        long id2 = idGenerator.generateId("input2");
        assertNotEquals(id1, id2);
    }
    
    @Test
    public void testGenerateFrontendId_ipv4() {
        long id = idGenerator.generateFrontendId("192.168.1.100", 8080);
        assertTrue(id != 0);
    }
    
    @Test
    public void testGenerateFrontendId_ipv6() {
        long id = idGenerator.generateFrontendId("2001:db8::1", 8080);
        assertTrue(id != 0);
    }
    
    @Test
    public void testGenerateFrontendId_sameAddress_sameId() {
        long id1 = idGenerator.generateFrontendId("192.168.1.100", 8080);
        long id2 = idGenerator.generateFrontendId("192.168.1.100", 8080);
        assertEquals(id1, id2);
    }
    
    @Test
    public void testGenerateFrontendId_differentPort_differentId() {
        long id1 = idGenerator.generateFrontendId("192.168.1.100", 8080);
        long id2 = idGenerator.generateFrontendId("192.168.1.100", 9090);
        assertNotEquals(id1, id2);
    }
    
    @Test
    public void testGenerateBackendId_normalCase() {
        long id = idGenerator.generateBackendId("10.0.0.1", 9000);
        assertTrue(id != 0);
    }
    
    @Test
    public void testGenerateNodeId_normalCase() {
        long id = idGenerator.generateNodeId("10", "node-01");
        assertTrue(id != 0);
    }
    
    @Test
    public void testGenerateNodeId_sameInput_sameId() {
        long id1 = idGenerator.generateNodeId("10", "node-01");
        long id2 = idGenerator.generateNodeId("10", "node-01");
        assertEquals(id1, id2);
    }
    
    @Test
    public void testGenerateNodeId_differentPrefix_differentId() {
        long id1 = idGenerator.generateNodeId("10", "node-01");
        long id2 = idGenerator.generateNodeId("20", "node-01");
        assertNotEquals(id1, id2);
    }
    
    @Test
    public void testIdGenerator_customSeed() {
        IdGenerator customGenerator = new IdGenerator(0xABCDEF);
        long id1 = idGenerator.generateId("test");
        long id2 = customGenerator.generateId("test");
        assertNotEquals(id1, id2);
    }
}