/**
 * SSL context factory for Netty.
 *
 * <p>Creates SslContext instances for server and client TLS configurations.
 * Supports:
 * <ul>
 *   <li>Server certificates (PEM format)</li>
 *   <li>Custom TLS protocols</li>
 *   <li>Custom cipher suites</li>
 *   <li>Classpath and filesystem certificate loading</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 * SslConfig config = SslConfig.builder()
 *     .enabled(true)
 *     .certFilePath("server.crt")
 *     .keyFilePath("server.key")
 *     .build();
 * SslContext sslContext = SslContextFactory.createServerContext(config);
 * </pre>
 */
public final class SslContextFactory {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SslContextFactory.class);
    
    private SslContextFactory() {
    }
    
    /**
     * Creates server SslContext from configuration.
     *
     * @param config SSL configuration
     * @return SslContext for server, or null if SSL disabled
     * @throws SSLException if SSL context creation fails
     * @throws IllegalArgumentException if certificate files not found
     */
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
    
    /**
     * Creates client SslContext from configuration.
     *
     * @param config SSL configuration
     * @return SslContext for client, or null if SSL disabled
     * @throws SSLException if SSL context creation fails
     */
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
    
    /**
     * Gets InputStream for certificate file.
     *
     * <p>First checks filesystem, then classpath.
     *
     * @param filePath certificate file path
     * @return InputStream for file
     * @throws IllegalArgumentException if file not found
     */
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