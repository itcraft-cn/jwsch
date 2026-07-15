import { FIXED_HEADER_LENGTH, MAX_TOPIC_LENGTH, isValidCommand } from './constants';

export class PacketHeader {
  constructor(options = {}) {
    this._command = options.command || 0;
    this._errorCode = options.errorCode || 0;
    this._sourceId = options.sourceId || 0;
    this._targetId = options.targetId || 0;
    this._topic = options.topic || null;
    this._bodyLength = options.bodyLength || 0;

    this._validate();

    const topicBytes = this._topic ? this._stringToAscii(this._topic) : [];
    this._headerLength = FIXED_HEADER_LENGTH + topicBytes.length;
  }

  get headerLength() {
    return this._headerLength;
  }

  get bodyLength() {
    return this._bodyLength;
  }

  get command() {
    return this._command;
  }

  get errorCode() {
    return this._errorCode;
  }

  get sourceId() {
    return this._sourceId;
  }

  get targetId() {
    return this._targetId;
  }

  get topic() {
    return this._topic;
  }

  get isSuccess() {
    return this._errorCode === 0;
  }

  _validate() {
    if (!isValidCommand(this._command)) {
      throw new Error(`Invalid command: ${this._command}`);
    }

    if (this._topic) {
      const topicLen = this._stringToAscii(this._topic).length;
      if (topicLen > MAX_TOPIC_LENGTH) {
        throw new Error(`Topic length exceeds max: ${MAX_TOPIC_LENGTH}`);
      }
    }
  }

  _stringToAscii(str) {
    const bytes = [];
    for (let i = 0; i < str.length; i++) {
      const code = str.charCodeAt(i);
      if (code < 128) {
        bytes.push(code);
      } else {
        bytes.push(63);
      }
    }
    return bytes;
  }

  static Builder = class {
    constructor() {
      this._command = 0;
      this._errorCode = 0;
      this._sourceId = 0;
      this._targetId = 0;
      this._topic = null;
      this._bodyLength = 0;
    }

    command(cmd) {
      this._command = cmd;
      return this;
    }

    errorCode(code) {
      this._errorCode = code;
      return this;
    }

    sourceId(id) {
      this._sourceId = id;
      return this;
    }

    targetId(id) {
      this._targetId = id;
      return this;
    }

    topic(t) {
      this._topic = t;
      return this;
    }

    bodyLength(len) {
      this._bodyLength = len;
      return this;
    }

    build() {
      return new PacketHeader({
        command: this._command,
        errorCode: this._errorCode,
        sourceId: this._sourceId,
        targetId: this._targetId,
        topic: this._topic,
        bodyLength: this._bodyLength
      });
    }
  };

  static builder() {
    return new PacketHeader.Builder();
  }
}