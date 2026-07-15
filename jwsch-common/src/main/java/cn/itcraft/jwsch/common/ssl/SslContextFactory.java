package cn.itcraft.jwsch.common.ssl;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;

public final class SslContextFactory {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SslContextFactory.class);
    
    private SslContextFactory() {
    }
    
    public static SslContext createServerContext(SslConfig config) throws SSLException {
        if (config == null || !config.isEnabled()) {
            return null;
        }
        
        LOGGER.info("Creating server SSL context: certFile={}", config.getCertFilePath());
        
        SslContextBuilder builder = SslContextBuilder.forServer(
            getFileInputStream(config.getCertFilePath()),
            getFileInputStream(config.getKeyFilePath()),
            config.getKeyPassword()
        );
        
        if (config.getProtocols() != null && config.getProtocols().length > 0) {
            builder.protocols(config.getProtocols());
        }
        
        if (config.getCipherSuites() != null && config.getCipherSuites().length > 0) {
            builder.ciphers(Arrays.asList(config.getCipherSuites()), SupportedCipherSuiteFilter.INSTANCE);
        }
        
        SslContext sslContext = builder.build();
        LOGGER.info("Server SSL context created successfully");
        
        return sslContext;
    }
    
    public static SslContext createClientContext(SslConfig config) throws SSLException {
        if (config == null || !config.isEnabled()) {
            return null;
        }
        
        LOGGER.info("Creating client SSL context");
        
        SslContextBuilder builder = SslContextBuilder.forClient();
        
        if (config.getProtocols() != null && config.getProtocols().length > 0) {
            builder.protocols(config.getProtocols());
        }
        
        if (config.getCipherSuites() != null && config.getCipherSuites().length > 0) {
            builder.ciphers(Arrays.asList(config.getCipherSuites()), SupportedCipherSuiteFilter.INSTANCE);
        }
        
        SslContext sslContext = builder.build();
        LOGGER.info("Client SSL context created successfully");
        
        return sslContext;
    }
    
    private static InputStream getFileInputStream(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        File file = new File(filePath);
        if (file.exists()) {
            try {
                return new FileInputStream(file);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to open file: " + filePath, e);
            }
        }
        
        InputStream classpathStream = SslContextFactory.class.getClassLoader()
            .getResourceAsStream(filePath);
        if (classpathStream != null) {
            return classpathStream;
        }
        
        throw new IllegalArgumentException("File not found: " + filePath);
    }
}