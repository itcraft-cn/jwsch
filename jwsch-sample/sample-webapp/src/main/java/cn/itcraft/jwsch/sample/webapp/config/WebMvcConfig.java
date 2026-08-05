package cn.itcraft.jwsch.sample.webapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类。
 * 
 * <p>配置 Spring MVC 相关设置，包括：
 * <ul>
 *   <li>CORS 跨域配置：允许所有来源访问 /api/** 接口</li>
 *   <li>静态资源处理：将 classpath:/static/ 目录映射到根路径</li>
 *   <li>视图控制器：支持前端路由，将未匹配的路径转发到 index.html</li>
 * </ul>
 * 
 * @author itcraft
 * @since 1.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    /**
     * 配置 CORS 跨域规则。
     * 
     * <p>允许所有来源访问 /api/** 接口，支持 GET/POST/PUT/DELETE 方法。
     * 
     * @param registry CORS 注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*");
    }
    
    /**
     * 配置静态资源处理器。
     * 
     * <p>将 classpath:/static/ 目录下的静态资源映射到根路径。
     * 
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/");
    }
    
    /**
     * 配置视图控制器。
     * 
     * <p>支持前端路由，将所有未匹配的路径转发到 index.html。
     * 这是单页应用（SPA）的常见配置模式。
     * 
     * @param registry 视图控制器注册表
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/{path:[^\\.]*}").setViewName("forward:/index.html");
    }
}