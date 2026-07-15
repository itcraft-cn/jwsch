import { Decoder } from '../../src/protocol/Decoder.js';
import { Encoder } from '../../src/protocol/Encoder.js';
import { Packet } from '../../src/protocol/Packet.js';
import { PacketHeader } from '../../src/protocol/PacketHeader.js';
import { Command, MAGIC } from '../../src/protocol/constants.js';

describe('Decoder', () => {
  let decoder;
  let encoder;

  beforeEach(() => {
    decoder = new Decoder();
    encoder = new Encoder();
  });

  describe('decode', () => {
    test('should decode encoded packet', () => {
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
      decoder.feed(buffer);
      const packets = decoder.decode();

      expect(packets.length).toBe(1);
      expect(packets[0].command).toBe(Command.REQUEST);
      expect(packets[0].sourceId).toBe(12345);
      expect(packets[0].targetId).toBe(67890);
      expect(packets[0].topic).toBe('/api/test');
      expect(packets[0].getBodyAsText()).toBe('hello');
    });

    test('should decode heartbeat packet', () => {
      const buffer = encoder.encodeHeartbeat(12345, 67890);
      decoder.feed(buffer);
      const packets = decoder.decode();

      expect(packets.length).toBe(1);
      expect(packets[0].command).toBe(Command.HEARTBEAT);
      expect(packets[0].sourceId).toBe(12345);
      expect(packets[0].targetId).toBe(67890);
    });

    test('should decode multiple packets', () => {
      const buffer1 = encoder.encodeHeartbeat(111, 222);
      const buffer2 = encoder.encodeHeartbeat(333, 444);

      decoder.feed(buffer1);
      decoder.feed(buffer2);
      const packets = decoder.decode();

      expect(packets.length).toBe(2);
      expect(packets[0].sourceId).toBe(111);
      expect(packets[1].sourceId).toBe(333);
    });

    test('should handle incomplete packet', () => {
      const buffer = encoder.encodeHeartbeat(12345, 67890);
      const partialBuffer = buffer.slice(0, 10);

      decoder.feed(partialBuffer);
      const packets = decoder.decode();

      expect(packets.length).toBe(0);
      expect(decoder.bufferedLength).toBe(10);
    });

    test('should throw error for invalid magic', () => {
      const buffer = new ArrayBuffer(27);
      const view = new DataView(buffer);
      view.setUint8(0, 0xFF);
      view.setUint8(1, 0xFF);

      decoder.feed(buffer);
      expect(() => decoder.decode()).toThrow('Invalid magic');
    });

    test('should decode packet with error code', () => {
      const header = PacketHeader.builder()
        .command(Command.RESPONSE)
        .errorCode(100)
        .sourceId(12345)
        .bodyLength(0)
        .build();
      const packet = new Packet(header, null);

      const buffer = encoder.encode(packet);
      decoder.feed(buffer);
      const packets = decoder.decode();

      expect(packets.length).toBe(1);
      expect(packets[0].errorCode).toBe(100);
      expect(packets[0].isSuccess).toBe(false);
    });
  });

  describe('bufferedLength', () => {
    test('should return 0 when no buffer', () => {
      expect(decoder.bufferedLength).toBe(0);
    });

    test('should return buffered length', () => {
      const buffer = encoder.encodeHeartbeat(12345, 67890);
      const partialBuffer = buffer.slice(0, 10);

      decoder.feed(partialBuffer);
      expect(decoder.bufferedLength).toBe(10);
    });
  });
});