package cn.itcraft.jwschd;

import cn.itcraft.jwsch.srv.cluster.ClusterConfig;
import cn.itcraft.jwsch.srv.config.JwschConfig;
import cn.itcraft.jwsch.srv.config.HealthConfig;
import cn.itcraft.jwsch.srv.config.MetricsConfig;
import cn.itcraft.jwsch.srv.config.TcpConfig;
import cn.itcraft.jwsch.srv.config.WebSocketConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class ConfigLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String DEFAULT_CONFIG = "application.yml";
    
    public static JwschConfig load(String[] args) {
        String configPath = findConfigPath(args);
        
        Map<String, Object> configMap = loadYaml(configPath);
        overrideFromEnv(configMap);
        overrideFromArgs(configMap, args);
        
        JwschConfig config = buildConfig(configMap);
        
        LOGGER.info("Configuration loaded");
        return config;
    }
    
    private static String findConfigPath(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--config".equals(args[i]) || "-c".equals(args[i])) {
                return args[i + 1];
            }
        }
        return null;
    }
    
    private static Map<String, Object> loadYaml(String path) {
        Yaml yaml = new Yaml();
        
        try (InputStream is = path != null 
            ? new FileInputStream(path) 
            : ConfigLoader.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG)) {
            
            if (is == null) {
                LOGGER.warn("No configuration file found, using defaults");
                return new HashMap<>();
            }
            
            Map<String, Object> map = yaml.load(is);
            return map != null ? map : new HashMap<>();
            
        } catch (Exception e) {
            LOGGER.warn("Failed to load config: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    private static void overrideFromEnv(Map<String, Object> config) {
        overrideEnvRecursive(config, "JWSCH_", "");
    }
    
    private static void overrideEnvRecursive(Map<String, Object> config, String prefix, String path) {
        for (String key : System.getenv().keySet()) {
            if (key.startsWith(prefix)) {
                String configKey = key.substring(prefix.length()).toLowerCase().replace("_", ".");
                setNestedValue(config, configKey, System.getenv(key));
            }
        }
    }
    
    private static void setNestedValue(Map<String, Object> config, String key, String value) {
        String[] parts = key.split("\\.");
        Map<String, Object> current = config;
        
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) {
                next = new HashMap<String, Object>();
                current.put(parts[i], next);
            }
            current = (Map<String, Object>) next;
        }
        
        current.put(parts[parts.length - 1], parseValue(value));
    }
    
    private static Object parseValue(String value) {
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }
    
    private static void overrideFromArgs(Map<String, Object> config, String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--jwsch.")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    setNestedValue(config, parts[0], parts[1]);
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private static JwschConfig buildConfig(Map<String, Object> map) {
        Map<String, Object> jwsch = getMap(map, "jwsch");
        
        WebSocketConfig.Builder wsBuilder = WebSocketConfig.builder();
        Map<String, Object> ws = getMap(jwsch, "websocket");
        if (ws != null) {
            wsBuilder.port(getInt(ws, "port", 8080))
                   .path(getString(ws, "path", "/ws"));
        }
        
        TcpConfig.Builder tcpBuilder = TcpConfig.builder();
        Map<String, Object> tcp = getMap(jwsch, "tcp");
        if (tcp != null) {
            tcpBuilder.port(getInt(tcp, "port", 9090));
        }
        
        ClusterConfig cluster = new ClusterConfig();
        Map<String, Object> clusterMap = getMap(jwsch, "cluster");
        if (clusterMap != null) {
            cluster.setEnabled(getBool(clusterMap, "enabled", false));
            cluster.setNodePrefix(getString(clusterMap, "node-prefix", "jwsch"));
            cluster.setBasePort(getInt(clusterMap, "base-port", 9090));
            cluster.setPortRange(getInt(clusterMap, "port-range", 3));
            cluster.setStartupWaitSeconds(getInt(clusterMap, "startup-wait-seconds", 5));
            cluster.setSyncIntervalSeconds(getInt(clusterMap, "sync-interval-seconds", 30));
            cluster.setHeartbeatIntervalSeconds(getInt(clusterMap, "heartbeat-interval-seconds", 10));
            cluster.setHeartbeatTimeoutSeconds(getInt(clusterMap, "heartbeat-timeout-seconds", 30));
        }
        
        HealthConfig.Builder healthBuilder = HealthConfig.builder();
        Map<String, Object> health = getMap(jwsch, "health");
        if (health != null) {
            healthBuilder.enabled(getBool(health, "enabled", false))
                       .port(getInt(health, "port", 8081));
        }
        
        MetricsConfig.Builder metricsBuilder = MetricsConfig.builder();
        Map<String, Object> metrics = getMap(jwsch, "metrics");
        if (metrics != null) {
            metricsBuilder.enabled(getBool(metrics, "enabled", false))
                        .port(getInt(metrics, "port", 8082))
                        .path(getString(metrics, "path", "/metrics"));
        }
        
        return JwschConfig.builder()
            .enabled(getBool(jwsch, "enabled", true))
            .bossThreads(getInt(jwsch, "boss-threads", 1))
            .workerThreads(getInt(jwsch, "worker-threads", 0))
            .webSocket(wsBuilder.build())
            .tcp(tcpBuilder.build())
            .cluster(cluster)
            .health(healthBuilder.build())
            .metrics(metricsBuilder.build())
            .build();
    }
    
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Map ? (Map<String, Object>) value : null;
    }
    
    private static boolean getBool(Map<String, Object> map, String key, boolean def) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : def;
    }
    
    private static int getInt(Map<String, Object> map, String key, int def) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).intValue() : def;
    }
    
    private static String getString(Map<String, Object> map, String key, String def) {
        Object value = map.get(key);
        return value instanceof String ? (String) value : def;
    }
}