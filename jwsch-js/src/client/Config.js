/**
 * JwschClient 配置类。
 * 
 * <p>参数说明：
 * - idleTime: 无业务流量后多久发心跳，默认 30s
 * - heartbeatTimeout: 心跳发送后多久无响应算超时，默认 30s
 * - retryTimes: 超时后重试次数，默认 3
 * - reconnect: 是否自动重连，默认 true
 * - reconnectInterval: 重连间隔，默认 3s
 * - maxReconnectAttempts: 最大重连次数，默认 5
 * - requestTimeout: 请求超时，默认 30s
 * 
 * <p>注意：
 * - keepalive/ssl 通过 ws:// 或 wss:// 区分，底层自动处理
 */
export class Config {
  constructor(options = {}) {
    this.idleTime = Config._parseInt(options.idleTime, 30000, 1000);
    this.heartbeatTimeout = Config._parseInt(options.heartbeatTimeout, 30000, 1000);
    this.retryTimes = Config._parseInt(options.retryTimes, 3, 1);
    
    this.reconnect = options.reconnect !== false;
    this.reconnectInterval = Config._parseInt(options.reconnectInterval, 3000, 1000);
    this.maxReconnectAttempts = Config._parseInt(options.maxReconnectAttempts, 5, 0);
    
    this.requestTimeout = Config._parseInt(options.requestTimeout, 30000, 1000);
    
    this.url = options.url || 'ws://localhost:8080/ws';
    this.debug = options.debug || false;
  }
  
  static _parseInt(value, defaultValue, minValue) {
    if (value === undefined || value === null) {
      return defaultValue;
    }
    const parsed = Number(value);
    if (!Number.isInteger(parsed) || parsed < minValue) {
      return defaultValue;
    }
    return parsed;
  }
}