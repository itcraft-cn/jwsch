import { Connection } from './Connection.js';
import { Heartbeat } from './Heartbeat.js';
import { Config } from './Config.js';
import { ResponseMapper } from '../router/ResponseMapper.js';
import { TopicSubscriber } from '../router/TopicSubscriber.js';
import { IdGenerator } from '../utils/IdGenerator.js';
import { Logger, LogLevel } from '../utils/Logger.js';
import { Command, ErrorCode } from '../protocol/constants.js';
import { PacketHeader } from '../protocol/PacketHeader.js';
import { Packet } from '../protocol/Packet.js';

/**
 * Jwsch JavaScript 客户端。
 * 
 * 支持功能：
 * - WebSocket 连接管理（自动重连）
 * - 心跳保活
 * - 请求-响应模式
 * - Topic 订阅/推送
 * 
 * @example
 * const client = new JwschClient({ url: 'ws://localhost:8080/ws' });
 * await client.connect();
 * await client.subscribe('/topic/news', (packet) => console.log(packet));
 * const response = await client.request('/api/user', { id: 1 });
 * client.disconnect();
 */
export class JwschClient {
  constructor(options = {}) {
    this._config = new Config(options);

    this._connection = null;
    this._heartbeat = null;
    this._responseMapper = new ResponseMapper(this._config.requestTimeout);
    this._topicSubscriber = new TopicSubscriber();
    this._idGenerator = new IdGenerator();
    this._connectionId = 0;
    this._eventHandlers = new Map();
    this._reconnectAttempts = 0;
    this._reconnectTimer = null;
    this._logger = new Logger('Jwsch', this._config.debug ? LogLevel.DEBUG : LogLevel.INFO);
  }

  connect() {
    if (this._connection && this._connection.isConnected) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      this._connection = new Connection(this._config.url, this._config);
      
      this._connection.onOpen(() => {
        this._reconnectAttempts = 0;
        this._startHeartbeat();
        this._connection.onActivity(() => {
          if (this._heartbeat) {
            this._heartbeat.onActivity();
          }
        });
        this._emit('open');
        resolve();
      });

      this._connection.onMessage((packet) => {
        this._handlePacket(packet);
      });

      this._connection.onClose((event) => {
        this._stopHeartbeat();
        this._responseMapper.cancelAll(new Error('Connection closed'));
        this._topicSubscriber.clear();
        this._emit('disconnected', event);

        if (this._config.reconnect && this._reconnectAttempts < this._config.maxReconnectAttempts) {
          this._scheduleReconnect();
        }
      });

      this._connection.onError((error) => {
        this._emit('error', error);
        if (!this._connection.isConnected) {
          reject(error);
        }
      });

      this._connection.connect().catch(reject);
    });
  }

  /**
   * 处理收到的 Packet。
   * 
   * 根据命令类型分发：
   * - CONNECT_RESPONSE：保存 connectionId
   * - RESPONSE：匹配请求-响应
   * - PUSH/BROADCAST：通知 Topic 订阅者
   * - HEARTBEAT：心跳响应
   */
  _handlePacket(packet) {
    const command = packet.command;
    this._logger.debug('Received packet, command:', command);

    switch (command) {
      case Command.CONNECT_RESPONSE:
        this._handleConnectResponse(packet);
        break;
      case Command.RESPONSE:
        this._handleResponse(packet);
        break;
      case Command.PUSH:
        this._handlePush(packet);
        break;
      case Command.BROADCAST:
        this._handleBroadcast(packet);
        break;
      case Command.HEARTBEAT:
        this._handleHeartbeat(packet);
        break;
      case Command.ACK:
        this._handleAck(packet);
        break;
      default:
        this._logger.warn('Unknown command:', command);
    }
  }

  _handleConnectResponse(packet) {
    this._connectionId = packet.sourceId;
    if (this._heartbeat) {
      this._heartbeat.setSourceId(this._connectionId);
    }
    this._logger.info('Received connectionId:', this._connectionId);
    this._emit('connected', this._connectionId);
  }

  _handleResponse(packet) {
    const sourceId = packet.sourceId;
    if (this._responseMapper.has(sourceId)) {
      this._responseMapper.resolve(sourceId, packet);
    } else {
      this._logger.warn('No pending request for response:', sourceId);
    }
  }

  _handlePush(packet) {
    const topic = packet.topic;
    if (topic) {
      this._topicSubscriber.notify(topic, packet);
    }
    this._emit('push', packet);
  }

  _handleBroadcast(packet) {
    const topic = packet.topic;
    if (topic) {
      this._topicSubscriber.notify(topic, packet);
    }
    this._emit('broadcast', packet);
  }

  _handleHeartbeat(packet) {
    if (this._heartbeat) {
      this._heartbeat.onResponse();
    }
  }

  _handleAck(packet) {
    this._emit('ack', packet);
  }

  _startHeartbeat() {
    this._heartbeat = new Heartbeat(this._connection, this._config);

    this._heartbeat.onTimeout(() => {
      this._logger.error('Heartbeat timeout, closing connection');
      this.disconnect();
      if (this._config.reconnect) {
        this._scheduleReconnect();
      }
    });

    this._heartbeat.start();
  }

  _stopHeartbeat() {
    if (this._heartbeat) {
      this._heartbeat.stop();
      this._heartbeat = null;
    }
  }

  _scheduleReconnect() {
    if (this._reconnectTimer) {
      return;
    }

    this._reconnectAttempts++;
    this._logger.info('Scheduling reconnect, attempt:', this._reconnectAttempts);

    this._reconnectTimer = setTimeout(() => {
      this._reconnectTimer = null;
      this.connect().catch((error) => {
        this._logger.error('Reconnect failed:', error);
      });
    }, this._config.reconnectInterval);
  }

  request(topic, body, timeout) {
    if (!this._connection || !this._connection.isConnected) {
      return Promise.reject(new Error('Not connected'));
    }

    const requestId = Number(this._idGenerator.nextId());
    const promise = this._responseMapper.register(requestId, timeout);

    try {
      this._connection.sendRequest({
        command: Command.REQUEST,
        sourceId: requestId,
        targetId: 0,
        topic: topic,
        body: body
      });
    } catch (error) {
      this._responseMapper.cancel(requestId);
      return Promise.reject(error);
    }

    return promise.then((packet) => {
      if (!packet.isSuccess) {
        const error = new Error(packet.getBodyAsText() || 'Request failed');
        error.code = packet.errorCode;
        throw error;
      }
      return packet;
    });
  }

  subscribe(topic, callback) {
    if (!this._connection || !this._connection.isConnected) {
      return Promise.reject(new Error('Not connected'));
    }

    this._topicSubscriber.subscribe(topic, callback);

    try {
      this._connection.sendSubscribe(topic, this._connectionId);
      return Promise.resolve();
    } catch (error) {
      this._topicSubscriber.unsubscribe(topic, callback);
      return Promise.reject(error);
    }
  }

  unsubscribe(topic, callback) {
    this._topicSubscriber.unsubscribe(topic, callback);
    return Promise.resolve();
  }

  push(topic, body) {
    if (!this._connection || !this._connection.isConnected) {
      throw new Error('Not connected');
    }

    this._connection.sendRequest({
      command: Command.PUSH,
      sourceId: Number(this._idGenerator.nextId()),
      targetId: 0,
      topic: topic,
      body: body
    });
  }

  broadcast(topic, body) {
    if (!this._connection || !this._connection.isConnected) {
      throw new Error('Not connected');
    }

    this._connection.sendRequest({
      command: Command.BROADCAST,
      sourceId: Number(this._idGenerator.nextId()),
      targetId: 0,
      topic: topic,
      body: body
    });
  }

  disconnect() {
    if (this._reconnectTimer) {
      clearTimeout(this._reconnectTimer);
      this._reconnectTimer = null;
    }

    this._stopHeartbeat();

    if (this._connection) {
      this._connection.disconnect();
      this._connection = null;
    }
  }

  on(event, handler) {
    if (!this._eventHandlers.has(event)) {
      this._eventHandlers.set(event, []);
    }
    this._eventHandlers.get(event).push(handler);
  }

  off(event, handler) {
    if (!this._eventHandlers.has(event)) {
      return;
    }
    const handlers = this._eventHandlers.get(event);
    const index = handlers.indexOf(handler);
    if (index >= 0) {
      handlers.splice(index, 1);
    }
  }

  _emit(event, ...args) {
    const handlers = this._eventHandlers.get(event);
    if (handlers) {
      for (const handler of handlers) {
        try {
          handler(...args);
        } catch (e) {
          this._logger.error('Event handler error:', e);
        }
      }
    }
  }

  get isConnected() {
    return this._connection && this._connection.isConnected;
  }

  get connectionId() {
    return this._connectionId;
  }
}