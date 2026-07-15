/**
 * 请求-响应映射器。
 * 
 * 管理请求 ID 到 Promise 的映射，支持超时处理。
 * 
 * @example
 * const mapper = new ResponseMapper(30000);
 * const promise = mapper.register(requestId); // 返回 Promise
 * mapper.resolve(requestId, packet); // 解析 Promise
 */
export class ResponseMapper {
  /**
   * @param {number} [timeout=30000] 默认超时时间（毫秒）
   */
  constructor(timeout = 30000) {
    /** @type {Map<number, Object>} 请求ID -> {resolve, reject, timer, timestamp} */
    this._pending = new Map();
    /** @type {number} 默认超时时间 */
    this._timeout = timeout;
  }

  /**
   * 注册一个请求，返回 Promise。
   * 
   * @param {number} requestId 请求ID
   * @param {number} [timeout] 超时时间，不传使用默认值
   * @returns {Promise<Packet>} 响应 Promise
   */
  register(requestId, timeout) {
    const actualTimeout = timeout || this._timeout;
    
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this._pending.delete(requestId);
        reject(new Error(`Request timeout: ${requestId}`));
      }, actualTimeout);

      this._pending.set(requestId, {
        resolve,
        reject,
        timer,
        timestamp: Date.now()
      });
    });
  }

  /**
   * 解析请求的 Promise。
   * 
   * @param {number} requestId 请求ID
   * @param {Packet} packet 响应数据包
   * @returns {boolean} 是否成功找到并解析
   */
  resolve(requestId, packet) {
    const entry = this._pending.get(requestId);
    if (!entry) {
      return false;
    }

    clearTimeout(entry.timer);
    this._pending.delete(requestId);
    entry.resolve(packet);
    return true;
  }

  reject(requestId, error) {
    const entry = this._pending.get(requestId);
    if (!entry) {
      return false;
    }

    clearTimeout(entry.timer);
    this._pending.delete(requestId);
    entry.reject(error);
    return true;
  }

  has(requestId) {
    return this._pending.has(requestId);
  }

  /**
   * 取消指定请求。
   * 
   * @param {number} requestId 请求ID
   */
  cancel(requestId) {
    const entry = this._pending.get(requestId);
    if (entry) {
      clearTimeout(entry.timer);
      this._pending.delete(requestId);
    }
  }

  /**
   * 取消所有待处理请求。
   * 
   * @param {Error} [error] 拒绝原因
   */
  cancelAll(error) {
    for (const [requestId, entry] of this._pending) {
      clearTimeout(entry.timer);
      entry.reject(error || new Error('Request cancelled'));
    }
    this._pending.clear();
  }

  get size() {
    return this._pending.size;
  }
}