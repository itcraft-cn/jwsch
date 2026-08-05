/**
 * SSL/TLS configuration.
 * 
 * <p>Provides SSL configuration for secure TCP connections.
 * Uses Builder pattern to create immutable configuration.
 * 
 * <p>Default protocols: TLSv1.2, TLSv1.3
 * Default cipher suites: null (uses JDK defaults)
 */
public final class SslConfig {
    
    private final boolean enabled;
    private final String certFilePath;
    private final String keyFilePath;
    private final String keyPassword;
    private final String[] protocols;
    private final String[] cipherSuites;
    
    private SslConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.certFilePath = builder.certFilePath;
        this.keyFilePath = builder.keyFilePath;
        this.keyPassword = builder.keyPassword;
        this.protocols = builder.protocols != null ? builder.protocols.clone() : null;
        this.cipherSuites = builder.cipherSuites != null ? builder.cipherSuites.clone() : null;
    }
    
    /**
     * Returns whether SSL is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Returns the certificate file path (PEM format).
     */
    public String getCertFilePath() {
        return certFilePath;
    }
    
    /**
     * Returns the private key file path (PEM format).
     */
    public String getKeyFilePath() {
        return keyFilePath;
    }
    
    /**
     * Returns the private key password (null if not encrypted).
     */
    public String getKeyPassword() {
        return keyPassword;
    }
    
    /**
     * Returns the enabled SSL/TLS protocols.
     * 
     * @return array of protocol names, or null for JDK defaults
     */
    public String[] getProtocols() {
        return protocols != null ? protocols.clone() : null;
    }
    
    /**
     * Returns the enabled cipher suites.
     * 
     * @return array of cipher suite names, or null for JDK defaults
     */
    public String[] getCipherSuites() {
        return cipherSuites != null ? cipherSuites.clone() : null;
    }
    
    /**
     * Builder for SslConfig.
     */
    public static final class Builder {
        private boolean enabled = false;
        private String certFilePath;
        private String keyFilePath;
        private String keyPassword;
        private String[] protocols = new String[]{"TLSv1.2", "TLSv1.3"};
        private String[] cipherSuites;
        
        /**
         * Sets whether SSL is enabled.
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        /**
         * Sets the certificate file path (PEM format).
         */
        public Builder certFilePath(String certFilePath) {
            this.certFilePath = certFilePath;
            return this;
        }
        
        /**
         * Sets the private key file path (PEM format).
         */
        public Builder keyFilePath(String keyFilePath) {
            this.keyFilePath = keyFilePath;
            return this;
        }
        
        /**
         * Sets the private key password (null if not encrypted).
         */
        public Builder keyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
            return this;
        }
        
        /**
         * Sets the SSL/TLS protocols (e.g., {"TLSv1.2", "TLSv1.3"}).
         * 
         * <p>If null, JDK defaults will be used.
         */
        public Builder protocols(String[] protocols) {
            this.protocols = protocols;
            return this;
        }
        
        /**
         * Sets the cipher suites (e.g., {"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"}).
         * 
         * <p>If null, JDK defaults will be used.
         */
        public Builder cipherSuites(String[] cipherSuites) {
            this.cipherSuites = cipherSuites;
            return this;
        }
        
        /**
         * Builds the SslConfig.
         * 
         * @throws NullPointerException if SSL enabled but certFilePath or keyFilePath is null
         */
        public SslConfig build() {
            if (enabled) {
                Objects.requireNonNull(certFilePath, "certFilePath is required when SSL is enabled");
                Objects.requireNonNull(keyFilePath, "keyFilePath is required when SSL is enabled");
            }
            return new SslConfig(this);
        }
    }
}