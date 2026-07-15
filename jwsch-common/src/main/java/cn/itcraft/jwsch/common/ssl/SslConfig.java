package cn.itcraft.jwsch.common.ssl;

import java.util.Objects;

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
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public String getCertFilePath() {
        return certFilePath;
    }
    
    public String getKeyFilePath() {
        return keyFilePath;
    }
    
    public String getKeyPassword() {
        return keyPassword;
    }
    
    public String[] getProtocols() {
        return protocols != null ? protocols.clone() : null;
    }
    
    public String[] getCipherSuites() {
        return cipherSuites != null ? cipherSuites.clone() : null;
    }
    
    public static final class Builder {
        private boolean enabled = false;
        private String certFilePath;
        private String keyFilePath;
        private String keyPassword;
        private String[] protocols = new String[]{"TLSv1.2", "TLSv1.3"};
        private String[] cipherSuites;
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public Builder certFilePath(String certFilePath) {
            this.certFilePath = certFilePath;
            return this;
        }
        
        public Builder keyFilePath(String keyFilePath) {
            this.keyFilePath = keyFilePath;
            return this;
        }
        
        public Builder keyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
            return this;
        }
        
        public Builder protocols(String[] protocols) {
            this.protocols = protocols;
            return this;
        }
        
        public Builder cipherSuites(String[] cipherSuites) {
            this.cipherSuites = cipherSuites;
            return this;
        }
        
        public SslConfig build() {
            if (enabled) {
                Objects.requireNonNull(certFilePath, "certFilePath is required when SSL is enabled");
                Objects.requireNonNull(keyFilePath, "keyFilePath is required when SSL is enabled");
            }
            return new SslConfig(this);
        }
    }
}