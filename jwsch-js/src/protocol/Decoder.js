import { MAGIC, FIXED_HEADER_LENGTH, MAX_BODY_LENGTH, isValidCommand } from './constants';
import { PacketHeader } from './PacketHeader';
import { Packet } from './Packet';

/**
 * 二进制协议解码器。
 * 
 * 支持粘包处理，内部维护缓冲区累积接收的数据。
 * 协议格式：
 * | Magic(2B) | HeaderLen(2B) | BodyLen(4B) | Cmd(1B) | ErrCode(2B) |
 * | SrcId(8B) | TgtId(8B) | Topic(NB) | Body(NB) |
 */
export class Decoder {
  constructor() {
    /** @type {ArrayBuffer|null} 内部缓冲区 */
    this._buffer = null;
    /** @type {number} 缓冲区长度 */
    this._bufferLength = 0;
  }

  /**
   * 向缓冲区添加数据。
   * 
   * @param {ArrayBuffer} data 接收到的二进制数据
   */
  feed(data) {
    if (this._buffer === null) {
      this._buffer = data.slice(0);
      this._bufferLength = data.byteLength;
    } else {
      const newBuffer = new ArrayBuffer(this._bufferLength + data.byteLength);
      const newView = new Uint8Array(newBuffer);
      newView.set(new Uint8Array(this._buffer), 0);
      newView.set(new Uint8Array(data), this._bufferLength);
      this._buffer = newBuffer;
      this._bufferLength = newBuffer.byteLength;
    }
  }

  /**
   * 尝试解码缓冲区中的数据。
   * 
   * 返回所有完整的 Packet，不完整的留在缓冲区中。
   * 
   * @returns {Packet[]} 解码后的 Packet 数组
   */
  decode() {
    const packets = [];

    while (this._bufferLength >= FIXED_HEADER_LENGTH) {
      const view = new DataView(this._buffer);

      const magic0 = view.getUint8(0);
      const magic1 = view.getUint8(1);

      if (magic0 !== MAGIC[0] || magic1 !== MAGIC[1]) {
        this._reset();
        throw new Error(`Invalid magic: [${magic0}, ${magic1}]`);
      }

      const headerLength = view.getUint16(2, false);
      const bodyLength = view.getUint32(4, false);
      const command = view.getUint8(8);
      const errorCode = view.getUint16(9, false);
      const sourceId = Number(view.getBigInt64(11, false));
      const targetId = Number(view.getBigInt64(19, false));

      if (headerLength < FIXED_HEADER_LENGTH) {
        this._reset();
        throw new Error(`Invalid header length: ${headerLength}`);
      }

      if (bodyLength < 0 || bodyLength > MAX_BODY_LENGTH) {
        this._reset();
        throw new Error(`Invalid body length: ${bodyLength}`);
      }

      if (!isValidCommand(command)) {
        this._reset();
        throw new Error(`Invalid command: ${command}`);
      }

      const totalLength = headerLength + bodyLength;
      if (this._bufferLength < totalLength) {
        break;
      }

      const topicLength = headerLength - FIXED_HEADER_LENGTH;
      let topic = null;
      if (topicLength > 0) {
        const topicBytes = new Uint8Array(this._buffer, FIXED_HEADER_LENGTH, topicLength);
        topic = this._asciiToString(topicBytes);
      }

      let body = null;
      if (bodyLength > 0) {
        body = this._buffer.slice(headerLength, headerLength + bodyLength);
      }

      const header = PacketHeader.builder()
        .command(command)
        .errorCode(errorCode)
        .sourceId(sourceId)
        .targetId(targetId)
        .topic(topic)
        .bodyLength(bodyLength)
        .build();

      packets.push(new Packet(header, body));

      if (this._bufferLength === totalLength) {
        this._reset();
      } else {
        const remaining = new ArrayBuffer(this._bufferLength - totalLength);
        new Uint8Array(remaining).set(new Uint8Array(this._buffer, totalLength));
        this._buffer = remaining;
        this._bufferLength = remaining.byteLength;
      }
    }

    return packets;
  }

  _reset() {
    this._buffer = null;
    this._bufferLength = 0;
  }

  _asciiToString(bytes) {
    let str = '';
    for (let i = 0; i < bytes.length; i++) {
      str += String.fromCharCode(bytes[i]);
    }
    return str;
  }

  get bufferedLength() {
    return this._bufferLength;
  }
}