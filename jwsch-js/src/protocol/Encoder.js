import { MAGIC } from './constants';
import { PacketHeader } from './PacketHeader';
import { Packet } from './Packet';

/**
 * 二进制协议编码器。
 * 
 * 将 Packet 对象编码为 ArrayBuffer，用于 WebSocket 发送。
 * 协议格式：
 * | Magic(2B) | HeaderLen(2B) | BodyLen(4B) | Cmd(1B) | ErrCode(2B) |
 * | SrcId(8B) | TgtId(8B) | Topic(NB) | Body(NB) |
 */
export class Encoder {
  /**
   * 编码 Packet 为 ArrayBuffer。
   * 
   * @param {Packet} packet 待编码的数据包
   * @returns {ArrayBuffer} 编码后的二进制数据
   */
  encode(packet) {
    const header = packet.header;
    const body = packet.body;

    const totalLength = header.headerLength + header.bodyLength;
    const buffer = new ArrayBuffer(totalLength);
    const view = new DataView(buffer);
    let offset = 0;

    view.setUint8(offset++, MAGIC[0]);
    view.setUint8(offset++, MAGIC[1]);

    view.setUint16(offset, header.headerLength, false);
    offset += 2;

    view.setUint32(offset, header.bodyLength, false);
    offset += 4;

    view.setUint8(offset++, header.command);

    view.setUint16(offset, header.errorCode, false);
    offset += 2;

    view.setBigInt64(offset, BigInt(header.sourceId), false);
    offset += 8;

    view.setBigInt64(offset, BigInt(header.targetId), false);
    offset += 8;

    if (header.topic) {
      const topicBytes = this._stringToAscii(header.topic);
      for (let i = 0; i < topicBytes.length; i++) {
        view.setUint8(offset++, topicBytes[i]);
      }
    }

    if (body && body.byteLength > 0) {
      const bodyView = new Uint8Array(buffer, offset);
      bodyView.set(new Uint8Array(body));
    }

    return buffer;
  }

  encodeRequest(options) {
    const body = options.body ? this._bodyToBytes(options.body) : null;
    const bodyLength = body ? body.byteLength : 0;

    const header = PacketHeader.builder()
      .command(options.command || 0x01)
      .errorCode(0)
      .sourceId(options.sourceId || 0)
      .targetId(options.targetId || 0)
      .topic(options.topic || null)
      .bodyLength(bodyLength)
      .build();

    return this.encode(new Packet(header, body));
  }

  encodeHeartbeat(sourceId, targetId) {
    const header = PacketHeader.builder()
      .command(0x06)
      .errorCode(0)
      .sourceId(sourceId)
      .targetId(targetId)
      .bodyLength(0)
      .build();

    return this.encode(new Packet(header, null));
  }

  encodeSubscribe(topic, sourceId) {
    const header = PacketHeader.builder()
      .command(0x05)
      .errorCode(0)
      .sourceId(sourceId)
      .targetId(0)
      .topic(topic)
      .bodyLength(0)
      .build();

    return this.encode(new Packet(header, null));
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

  _bodyToBytes(body) {
    if (body instanceof ArrayBuffer) {
      return body;
    }
    if (body instanceof Uint8Array) {
      return body.buffer;
    }
    if (typeof body === 'string') {
      return new TextEncoder().encode(body).buffer;
    }
    if (typeof body === 'object') {
      return new TextEncoder().encode(JSON.stringify(body)).buffer;
    }
    return null;
  }
}