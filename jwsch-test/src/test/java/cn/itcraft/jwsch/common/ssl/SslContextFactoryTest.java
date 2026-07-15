package cn.itcraft.jwsch.common.ssl;

import io.netty.handler.ssl.SslContext;
import org.junit.Test;

import javax.net.ssl.SSLException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class SslContextFactoryTest {
    
    @Test
    public void testCreateServerContextWithNullConfig() throws SSLException {
        SslContext context = SslContextFactory.createServerContext(null);
        assertNull(context);
    }
    
    @Test
    public void testCreateServerContextWithDisabledConfig() throws SSLException {
        SslConfig config = new SslConfig.Builder()
            .enabled(false)
            .build();
        
        SslContext context = SslContextFactory.createServerContext(config);
        assertNull(context);
    }
    
    @Test
    public void testCreateClientContextWithNullConfig() throws SSLException {
        SslContext context = SslContextFactory.createClientContext(null);
        assertNull(context);
    }
    
    @Test
    public void testCreateClientContextWithDisabledConfig() throws SSLException {
        SslConfig config = new SslConfig.Builder()
            .enabled(false)
            .build();
        
        SslContext context = SslContextFactory.createClientContext(config);
        assertNull(context);
    }
    
    @Test
    public void testCreateClientContextWithValidConfig() throws SSLException {
        SslConfig config = new SslConfig.Builder()
            .enabled(true)
            .certFilePath("/dummy/cert.pem")
            .keyFilePath("/dummy/key.pem")
            .build();
        
        SslContext context = SslContextFactory.createClientContext(config);
        assertNotNull(context);
    }
    
    @Test
    public void testGetFileInputStreamWithNullPath() throws Exception {
        Method method = SslContextFactory.class.getDeclaredMethod("getFileInputStream", String.class);
        method.setAccessible(true);
        
        try {
            method.invoke(null, (String) null);
            fail("Expected IllegalArgumentException");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }
    
    @Test
    public void testGetFileInputStreamWithEmptyPath() throws Exception {
        Method method = SslContextFactory.class.getDeclaredMethod("getFileInputStream", String.class);
        method.setAccessible(true);
        
        try {
            method.invoke(null, "");
            fail("Expected IllegalArgumentException");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }
    
    @Test
    public void testGetFileInputStreamWithNonExistentFile() throws Exception {
        Method method = SslContextFactory.class.getDeclaredMethod("getFileInputStream", String.class);
        method.setAccessible(true);
        
        try {
            method.invoke(null, "/non/existent/file.pem");
            fail("Expected IllegalArgumentException");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }
}