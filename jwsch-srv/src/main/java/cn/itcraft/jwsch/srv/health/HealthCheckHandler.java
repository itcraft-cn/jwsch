package cn.itcraft.jwsch.srv.health;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

class HealthCheckHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckHandler.class);
    private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
    
    private final HealthAggregator healthAggregator;
    
    HealthCheckHandler(HealthAggregator healthAggregator) {
        this.healthAggregator = healthAggregator;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        String path = uri.split("\\?")[0];
        
        FullHttpResponse response;
        
        if ("/health/live".equals(path)) {
            response = createLivenessResponse();
        } else if ("/health/ready".equals(path)) {
            response = createReadinessResponse();
        } else if ("/health".equals(path)) {
            response = createHealthResponse();
        } else {
            response = createNotFoundResponse();
        }
        
        sendResponse(ctx, request, response);
    }
    
    private FullHttpResponse createLivenessResponse() {
        String json = "{\"status\":\"UP\"}";
        return createJsonResponse(HttpResponseStatus.OK, json);
    }
    
    private FullHttpResponse createReadinessResponse() {
        HealthInfo info = healthAggregator.checkHealth();
        String json = buildHealthJson(info);
        HttpResponseStatus status = info.getStatus() == HealthStatus.UP 
            ? HttpResponseStatus.OK 
            : HttpResponseStatus.SERVICE_UNAVAILABLE;
        return createJsonResponse(status, json);
    }
    
    private FullHttpResponse createHealthResponse() {
        HealthInfo info = healthAggregator.checkHealth();
        String json = buildHealthJson(info);
        HttpResponseStatus status = info.getStatus() == HealthStatus.UP 
            ? HttpResponseStatus.OK 
            : HttpResponseStatus.SERVICE_UNAVAILABLE;
        return createJsonResponse(status, json);
    }
    
    private FullHttpResponse createNotFoundResponse() {
        String json = "{\"error\":\"Not Found\"}";
        return createJsonResponse(HttpResponseStatus.NOT_FOUND, json);
    }
    
    private FullHttpResponse createJsonResponse(HttpResponseStatus status, String content) {
        ByteBuf buffer = Unpooled.copiedBuffer(content, StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, status, buffer);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, CONTENT_TYPE_JSON);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buffer.readableBytes());
        return response;
    }
    
    private String buildHealthJson(HealthInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"status\":\"").append(info.getStatus().name()).append("\"");
        
        Map<String, HealthStatus> components = info.getComponents();
        if (!components.isEmpty()) {
            sb.append(",\"components\":{");
            boolean first = true;
            for (Map.Entry<String, HealthStatus> entry : components.entrySet()) {
                if (!first) {
                    sb.append(",");
                }
                sb.append("\"").append(entry.getKey()).append("\":\"")
                    .append(entry.getValue().name()).append("\"");
                first = false;
            }
            sb.append("}");
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    private void sendResponse(ChannelHandlerContext ctx, FullHttpRequest request, 
                               FullHttpResponse response) {
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        
        ChannelFuture future = ctx.writeAndFlush(response);
        
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Health check error", cause);
        ctx.close();
    }
}