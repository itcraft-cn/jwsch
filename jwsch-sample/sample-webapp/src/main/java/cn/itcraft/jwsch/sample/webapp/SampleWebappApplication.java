package cn.itcraft.jwsch.sample.webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 示例 Web 应用入口。
 * 
 * <p>Spring Boot 应用主类，启动内置 Web 服务器和调度功能。
 * 提供静态页面展示 jwsch 的 WebSocket 功能。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>静态资源服务：提供前端 HTML/JS/CSS 文件</li>
 *   <li>RESTful API：提供应用信息和健康检查接口</li>
 *   <li>WebSocket 客户端演示：连接 jwsch 服务器并展示实时数据</li>
 * </ul>
 * 
 * @author itcraft
 * @since 1.0
 */
@SpringBootApplication
@EnableScheduling
public class SampleWebappApplication {
    
    /**
     * Spring Boot 应用主入口方法。
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SampleWebappApplication.class, args);
    }
}