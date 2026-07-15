import { Platform } from '../utils/Platform.js';
import { Decoder } from '../protocol/Decoder.js';
import { Encoder } from '../protocol/Encoder.js';
import { Command } from '../protocol/constants.js';
import logger from '../utils/Logger.js';

export class Connection {
  constructor(url, options = {}) {
    this._url = url;
    this._options = options;
    this._ws = null;
    this._connected = false;
    this._encoder = new Encoder();
    this._decoder = new Decoder();
    this._onMessage = null;
    this._onOpen = null;
    this._onClose = null;
    this._onError = null;
    this._onActivity = null;
  }

  connect() {
    return new Promise((resolve, reject) => {
      try {
        const WebSocketClass = Platform.getWebSocket();
        this._ws = new WebSocketClass(this._url);
        
        this._ws.binaryType = 'arraybuffer';

        this._ws.onopen = () => {
          this._connected = true;
          logger.info('WebSocket connected:', this._url);
          if (this._onOpen) this._onOpen();
          resolve();
        };

        this._ws.onmessage = (event) => {
          try {
            const data = Platform.isBrowser() ? event.data : event.data;
            const arrayBuffer = data instanceof ArrayBuffer 
              ? data 
              : data.buffer || data;
            
            this._decoder.feed(arrayBuffer);
            const packets = this._decoder.decode();
            
            for (const packet of packets) {
              if (this._onMessage) {
                this._onMessage(packet);
              }
            }
          } catch (e) {
            logger.error('Message decode error:', e);
          }
        };

        this._ws.onclose = (event) => {
          this._connected = false;
          logger.info('WebSocket closed:', event.code, event.reason);
          if (this._onClose) this._onClose(event);
        };

        this._ws.onerror = (error) => {
          logger.error('WebSocket error:', error);
          if (this._onError) this._onError(error);
          if (!this._connected) {
            reject(error);
          }
        };

      } catch (e) {
        reject(e);
      }
    });
  }

  send(buffer) {
    if (!this._connected || !this._ws) {
      throw new Error('WebSocket is not connected');
    }

    this._ws.send(buffer);
    if (this._onActivity) {
      this._onActivity();
    }
  }

  sendPacket(packet) {
    const buffer = this._encoder.encode(packet);
    this.send(buffer);
  }

  sendRequest(options) {
    const buffer = this._encoder.encodeRequest(options);
    this.send(buffer);
  }

  sendHeartbeat(sourceId, targetId) {
    const buffer = this._encoder.encodeHeartbeat(sourceId, targetId);
    this.send(buffer);
  }

  sendSubscribe(topic, sourceId) {
    const buffer = this._encoder.encodeSubscribe(topic, sourceId);
    this.send(buffer);
  }

  disconnect() {
    if (this._ws) {
      this._connected = false;
      this._ws.close();
      this._ws = null;
    }
  }

  get isConnected() {
    return this._connected;
  }

  get url() {
    return this._url;
  }

  onMessage(callback) {
    this._onMessage = callback;
  }

  onOpen(callback) {
    this._onOpen = callback;
  }

  onClose(callback) {
    this._onClose = callback;
  }

  onError(callback) {
    this._onError = callback;
  }
  
  onActivity(callback) {
    this._onActivity = callback;
  }
}