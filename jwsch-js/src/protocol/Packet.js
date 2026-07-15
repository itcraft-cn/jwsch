import { PacketHeader } from './PacketHeader';

export class Packet {
  constructor(header, body) {
    this._header = header;
    this._body = body || null;
  }

  get header() {
    return this._header;
  }

  get body() {
    return this._body;
  }

  get command() {
    return this._header.command;
  }

  get errorCode() {
    return this._header.errorCode;
  }

  get sourceId() {
    return this._header.sourceId;
  }

  get targetId() {
    return this._header.targetId;
  }

  get topic() {
    return this._header.topic;
  }

  get isSuccess() {
    return this._header.isSuccess;
  }

  hasBody() {
    return this._body !== null && this._body.byteLength > 0;
  }

  getBodyAsText() {
    if (!this.hasBody()) {
      return null;
    }
    const decoder = new TextDecoder('utf-8');
    return decoder.decode(this._body);
  }

  getBodyAsJson() {
    const text = this.getBodyAsText();
    if (!text) {
      return null;
    }
    try {
      return JSON.parse(text);
    } catch (e) {
      return null;
    }
  }

  static from(header, body) {
    return new Packet(header, body);
  }
}