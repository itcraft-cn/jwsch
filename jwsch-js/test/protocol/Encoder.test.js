import { Encoder } from '../../src/protocol/Encoder.js';
import { Packet } from '../../src/protocol/Packet.js';
import { PacketHeader } from '../../src/protocol/PacketHeader.js';
import { Command, MAGIC, FIXED_HEADER_LENGTH } from '../../src/protocol/constants.js';

describe('Encoder', () => {
  let encoder;

  beforeEach(() => {
    encoder = new Encoder();
  });

  describe('encode', () => {
    test('should encode packet correctly', () => {
      const header = PacketHeader.builder()
        .command(Command.REQUEST)
        .errorCode(0)
        .sourceId(12345)
        .targetId(67890)
        .topic('/api/test')
        .bodyLength(5)
        .build();
      const body = new TextEncoder().encode('hello').buffer;
      const packet = new Packet(header, body);

      const buffer = encoder.encode(packet);
      const view = new DataView(buffer);

      expect(view.getUint8(0)).toBe(MAGIC[0]);
      expect(view.getUint8(1)).toBe(MAGIC[1]);
      expect(view.getUint16(2, false)).toBe(header.headerLength);
      expect(view.getUint32(4, false)).toBe(5);
      expect(view.getUint8(8)).toBe(Command.REQUEST);
      expect(view.getUint16(9, false)).toBe(0);
      expect(Number(view.getBigInt64(11, false))).toBe(12345);
      expect(Number(view.getBigInt64(19, false))).toBe(67890);
    });

    test('should encode packet without body', () => {
      const header = PacketHeader.builder()
        .command(Command.HEARTBEAT)
        .sourceId(12345)
        .bodyLength(0)
        .build();
      const packet = new Packet(header, null);

      const buffer = encoder.encode(packet);
      expect(buffer.byteLength).toBe(FIXED_HEADER_LENGTH);
    });

    test('should encode topic correctly', () => {
      const header = PacketHeader.builder()
        .command(Command.SUBSCRIBE)
        .topic('/topic/news')
        .bodyLength(0)
        .build();
      const packet = new Packet(header, null);

      const buffer = encoder.encode(packet);
      const view = new DataView(buffer);
      
      const topicLength = 11;
      expect(buffer.byteLength).toBe(FIXED_HEADER_LENGTH + topicLength);
      
      const topicBytes = new Uint8Array(buffer, FIXED_HEADER_LENGTH, topicLength);
      const topic = String.fromCharCode(...topicBytes);
      expect(topic).toBe('/topic/news');
    });
  });

  describe('encodeRequest', () => {
    test('should encode request with JSON body', () => {
      const buffer = encoder.encodeRequest({
        command: Command.REQUEST,
        sourceId: 12345,
        topic: '/api/user/get',
        body: { userId: 1 }
      });

      const view = new DataView(buffer);
      expect(view.getUint8(8)).toBe(Command.REQUEST);
      expect(Number(view.getBigInt64(11, false))).toBe(12345);
    });
  });

  describe('encodeHeartbeat', () => {
    test('should encode heartbeat packet', () => {
      const buffer = encoder.encodeHeartbeat(12345, 67890);
      const view = new DataView(buffer);

      expect(view.getUint8(8)).toBe(Command.HEARTBEAT);
      expect(Number(view.getBigInt64(11, false))).toBe(12345);
      expect(Number(view.getBigInt64(19, false))).toBe(67890);
      expect(buffer.byteLength).toBe(FIXED_HEADER_LENGTH);
    });
  });

  describe('encodeSubscribe', () => {
    test('should encode subscribe packet', () => {
      const buffer = encoder.encodeSubscribe('/topic/news', 12345);
      const view = new DataView(buffer);

      expect(view.getUint8(8)).toBe(Command.SUBSCRIBE);
      expect(Number(view.getBigInt64(11, false))).toBe(12345);
    });
  });
});