package cn.itcraft.jwsch.common.ssl;

import org.junit.Test;

import static org.junit.Assert.*;

public class SslConfigTest {
    
    @Test
    public void testDefaultValues() {
        SslConfig config = new SslConfig.Builder().build();
        
        assertFalse(config.isEnabled());
        assertNull(config.getCertFilePath());
        assertNull(config.getKeyFilePath());
        assertNull(config.getKeyPassword());
        assertArrayEquals(new String[]{"TLSv1.2", "TLSv1.3"}, config.getProtocols());
        assertNull(config.getCipherSuites());
    }
    
    @Test
    public void testBuilderWithCustomValues() {
        String[] protocols = new String[]{"TLSv1.3"};
        String[] cipherSuites = new String[]{"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"};
        
        SslConfig config = new SslConfig.Builder()
            .enabled(true)
            .certFilePath("/path/to/cert.pem")
            .keyFilePath("/path/to/key.pem")
            .keyPassword("password")
            .protocols(protocols)
            .cipherSuites(cipherSuites)
            .build();
        
        assertTrue(config.isEnabled());
        assertEquals("/path/to/cert.pem", config.getCertFilePath());
        assertEquals("/path/to/key.pem", config.getKeyFilePath());
        assertEquals("password", config.getKeyPassword());
        assertArrayEquals(new String[]{"TLSv1.3"}, config.getProtocols());
        assertArrayEquals(new String[]{"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"}, config.getCipherSuites());
    }
    
    @Test(expected = NullPointerException.class)
    public void testEnabledWithoutCertFilePath() {
        new SslConfig.Builder()
            .enabled(true)
            .keyFilePath("/path/to/key.pem")
            .build();
    }
    
    @Test(expected = NullPointerException.class)
    public void testEnabledWithoutKeyFilePath() {
        new SslConfig.Builder()
            .enabled(true)
            .certFilePath("/path/to/cert.pem")
            .build();
    }
    
    @Test
    public void testProtocolsDefensiveCopy() {
        String[] originalProtocols = new String[]{"TLSv1.2", "TLSv1.3"};
        
        SslConfig config = new SslConfig.Builder()
            .protocols(originalProtocols)
            .build();
        
        originalProtocols[0] = "TLSv1.0";
        
        assertArrayEquals(new String[]{"TLSv1.2", "TLSv1.3"}, config.getProtocols());
    }
    
    @Test
    public void testCipherSuitesDefensiveCopy() {
        String[] originalCipherSuites = new String[]{"CIPHER1", "CIPHER2"};
        
        SslConfig config = new SslConfig.Builder()
            .cipherSuites(originalCipherSuites)
            .build();
        
        originalCipherSuites[0] = "CIPHER_MODIFIED";
        
        assertArrayEquals(new String[]{"CIPHER1", "CIPHER2"}, config.getCipherSuites());
    }
    
    @Test
    public void testGetProtocolsDefensiveCopy() {
        SslConfig config = new SslConfig.Builder()
            .protocols(new String[]{"TLSv1.2", "TLSv1.3"})
            .build();
        
        String[] protocols1 = config.getProtocols();
        String[] protocols2 = config.getProtocols();
        
        assertNotSame(protocols1, protocols2);
    }
    
    @Test
    public void testGetCipherSuitesDefensiveCopy() {
        SslConfig config = new SslConfig.Builder()
            .cipherSuites(new String[]{"CIPHER1", "CIPHER2"})
            .build();
        
        String[] cipherSuites1 = config.getCipherSuites();
        String[] cipherSuites2 = config.getCipherSuites();
        
        assertNotSame(cipherSuites1, cipherSuites2);
    }
}