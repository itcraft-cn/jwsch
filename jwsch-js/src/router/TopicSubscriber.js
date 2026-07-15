/**
 * Topic 订阅管理器。
 * 
 * 管理 Topic 到回调函数的映射关系。
 * 
 * @example
 * const subscriber = new TopicSubscriber();
 * subscriber.subscribe('/topic/news', (packet) => console.log(packet));
 * subscriber.notify('/topic/news', packet); // 调用所有订阅者
 */
export class TopicSubscriber {
  constructor() {
    /** @type {Map<string, Set<Function>>} Topic -> 回调函数集合 */
    this._subscriptions = new Map();
  }

  /**
   * 订阅 Topic。
   * 
   * @param {string} topic Topic 名称
   * @param {Function} callback 回调函数
   * @returns {boolean} 是否成功
   */
  subscribe(topic, callback) {
    if (!this._subscriptions.has(topic)) {
      this._subscriptions.set(topic, new Set());
    }
    this._subscriptions.get(topic).add(callback);
    return true;
  }

  /**
   * 取消订阅。
   * 
   * @param {string} topic Topic 名称
   * @param {Function} [callback] 可选，不传则取消该 Topic 所有订阅
   * @returns {boolean} 是否成功
   */
  unsubscribe(topic, callback) {
    if (!this._subscriptions.has(topic)) {
      return false;
    }

    if (callback) {
      const callbacks = this._subscriptions.get(topic);
      callbacks.delete(callback);
      if (callbacks.size === 0) {
        this._subscriptions.delete(topic);
      }
    } else {
      this._subscriptions.delete(topic);
    }
    return true;
  }

  /**
   * 通知 Topic 的所有订阅者。
   * 
   * @param {string} topic Topic 名称
   * @param {Packet} packet 数据包
   * @returns {number} 被调用的回调数量
   */
  notify(topic, packet) {
    const callbacks = this._subscriptions.get(topic);
    if (!callbacks || callbacks.size === 0) {
      return 0;
    }

    for (const callback of callbacks) {
      try {
        callback(packet);
      } catch (e) {
        console.error('Topic callback error:', e);
      }
    }
    return callbacks.size;
  }

  has(topic) {
    return this._subscriptions.has(topic);
  }

  getTopics() {
    return Array.from(this._subscriptions.keys());
  }

  clear() {
    this._subscriptions.clear();
  }

  get size() {
    return this._subscriptions.size;
  }
}