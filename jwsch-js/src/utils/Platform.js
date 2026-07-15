export const Platform = {
  isNode() {
    return typeof process !== 'undefined'
      && process.versions != null
      && process.versions.node != null;
  },

  isBrowser() {
    return typeof window !== 'undefined' && typeof document !== 'undefined';
  },

  getWebSocket() {
    if (this.isBrowser()) {
      return window.WebSocket;
    }
    if (this.isNode()) {
      return require('ws');
    }
    throw new Error('WebSocket is not available in this environment');
  },

  getArrayBuffer() {
    return ArrayBuffer;
  },

  getTextEncoder() {
    if (typeof TextEncoder !== 'undefined') {
      return TextEncoder;
    }
    if (this.isNode()) {
      return require('util').TextEncoder;
    }
    throw new Error('TextEncoder is not available');
  },

  getTextDecoder() {
    if (typeof TextDecoder !== 'undefined') {
      return TextDecoder;
    }
    if (this.isNode()) {
      return require('util').TextDecoder;
    }
    throw new Error('TextDecoder is not available');
  }
};