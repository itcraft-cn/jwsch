import logger from '../utils/Logger.js';

/**
 * 心跳管理器。
 * 
 * <p>功能：
 * <ul>
 *   <li>流量检测：有业务流量时跳过心跳</li>
 *   <li>空闲发送：idleTime 内无流量则发送心跳</li>
 *   <li>超时重试：心跳超时后重试 retryTimes 次</li>
 * </ul>
 * 
 * <p>心跳机制：
 * <pre>
 * 启动 → 记录最后活动时间 → 延迟 idleTime 检查
 *   ↓ 有流量 → 更新活动时间 → 延迟 idleTime 再检查
 *   ↓ 无流量 → 发送心跳 → 等待 heartbeatTimeout
 *       ↓ 有响应 → 重置重试计数
 *       ↓ 无响应 → 重试 retryTimes 次后触发超时回调
 * </pre>
 */
export class Heartbeat {
  constructor(connection, config) {
    this._connection = connection;
    this._config = config;
    this._sourceId = 0;
    this._timer = null;
    this._timeoutTimer = null;
    this._lastActivityTime = 0;
    this._retryCount = 0;
    this._onTimeout = null;
  }

  start() {
    if (this._timer) {
      return;
    }

    this._lastActivityTime = Date.now();
    this._retryCount = 0;
    this._scheduleNext();
    logger.debug('Heartbeat started, idleTime:', this._config.idleTime);
  }

  stop() {
    this._cancelTimer();
    this._cancelTimeoutTimer();
    logger.debug('Heartbeat stopped');
  }

  _cancelTimer() {
    if (this._timer) {
      clearTimeout(this._timer);
      this._timer = null;
    }
  }

  _cancelTimeoutTimer() {
    if (this._timeoutTimer) {
      clearTimeout(this._timeoutTimer);
      this._timeoutTimer = null;
    }
  }

  _scheduleNext() {
    this._cancelTimer();
    
    this._timer = setTimeout(() => {
      this._checkAndSend();
    }, this._config.idleTime);
  }

  _checkAndSend() {
    if (!this._connection.isConnected) {
      this.stop();
      return;
    }

    const elapsed = Date.now() - this._lastActivityTime;
    if (elapsed < this._config.idleTime) {
      this._scheduleNext();
      return;
    }

    this._sendHeartbeat();
  }

  _sendHeartbeat() {
    if (!this._connection.isConnected) {
      this.stop();
      return;
    }

    try {
      this._connection.sendHeartbeat(this._sourceId, 0);
      logger.debug('Heartbeat sent, sourceId:', this._sourceId);
      this._scheduleTimeout();
    } catch (e) {
      logger.error('Failed to send heartbeat:', e);
      this._handleTimeout();
    }
  }

  _scheduleTimeout() {
    this._cancelTimeoutTimer();
    
    this._timeoutTimer = setTimeout(() => {
      this._handleTimeout();
    }, this._config.heartbeatTimeout);
  }

  _handleTimeout() {
    this._retryCount++;
    
    if (this._retryCount >= this._config.retryTimes) {
      logger.error('Heartbeat timeout, max retry reached:', this._retryCount);
      this.stop();
      if (this._onTimeout) {
        this._onTimeout();
      }
      return;
    }

    logger.warn('Heartbeat timeout, retrying:', this._retryCount, '/', this._config.retryTimes);
    this._sendHeartbeat();
  }

  onResponse() {
    this._cancelTimeoutTimer();
    this._retryCount = 0;
    this._lastActivityTime = Date.now();
    this._scheduleNext();
    logger.debug('Heartbeat response received');
  }

  onActivity() {
    this._lastActivityTime = Date.now();
    this._cancelTimeoutTimer();
  }

  setSourceId(id) {
    this._sourceId = id;
  }

  onTimeout(callback) {
    this._onTimeout = callback;
  }

  get isRunning() {
    return this._timer !== null;
  }

  get lastActivityTime() {
    return this._lastActivityTime;
  }
}