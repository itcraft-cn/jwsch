package cn.itcraft.jwsch.sample.webapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * API 控制器。
 * 
 * <p>提供示例 Web 应用的 RESTful API 接口。
 * 所有接口都位于 /api 路径下。
 * 
 * @author itcraft
 * @since 1.0
 */
@RestController
@RequestMapping("/api")
public class ApiController {
    
    /**
     * 获取应用信息。
     * 
     * @return 包含应用名称、版本、WebSocket 地址和时间戳的信息
     */
    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "jwsch-sample-webapp");
        info.put("version", "1.0.0");
        info.put("websocket", "ws://localhost:8080/ws");
        info.put("timestamp", System.currentTimeMillis());
        return info;
    }
    
    /**
     * 健康检查接口。
     * 
     * @return 包含状态和时间戳的健康信息
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        return health;
    }
}