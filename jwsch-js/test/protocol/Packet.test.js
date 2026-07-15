import { Packet } from '../../src/protocol/Packet.js';
import { PacketHeader } from '../../src/protocol/PacketHeader.js';
import { Command } from '../../src/protocol/constants.js';

describe('Packet', () => {
  describe('constructor', () => {
    test('should create packet with header and body', () => {
      const header = PacketHeader.builder()
        .command(Command.REQUEST)
        .sourceId(12345)
        .build();
      const body = new TextEncoder().encode('test').buffer;
      const packet = new Packet(header, body);

      expect(packet.header).toBe(header);
      expect(packet.body).toBe(body);
      expect(packet.command).toBe(Command.REQUEST);
      expect(packet.sourceId).toBe(12345);
    });

    test('should create packet with null body', () => {
      const header = PacketHeader.builder()
        .command(Command.HEARTBEAT)
        .build();
      const packet = new Packet(header, null);

      expect(packet.header).toBe(header);
      expect(packet.body).toBeNull();
    });
  });

  describe('hasBody', () => {
    test('should return true when body exists', () => {
      const header = PacketHeader.builder().command(Command.REQUEST).build();
      const body = new TextEncoder().encode('test').buffer;
      const packet = new Packet(header, body);
      expect(packet.hasBody()).toBe(true);
    });

    test('should return false when body is null', () => {
      const header = PacketHeader.builder().command(Command.REQUEST).build();
      const packet = new Packet(header, null);
      expect(packet.hasBody()).toBe(false);
    });
  });

  describe('getBodyAsText', () => {
    test('should return body as text', () => {
      const header = PacketHeader.builder().command(Command.REQUEST).build();
      const body = new TextEncoder().encode('hello world').buffer;
      const packet = new Packet(header, body);
      expect(packet.getBodyAsText()).toBe('hello world');
    });

    test('should return null when no body', () => {
      const header = PacketHeader.builder().command(Command.REQUEST).build();
      const packet = new Packet(header, null);
      expect(packet.getBodyAsText()).toBeNull();
    });
  });

  describe('getBodyAsJson', () => {
    test('should return body as JSON', () => {
      const header = PacketHeader.builder().command(Command.REQUEST).build();
      const body = new TextEncoder().encode('{"name":"test"}').buffer;
      const packet = new Packet(header, body);
      expect(packet.getBodyAsJson()).toEqual({ name: 'test' });
    });

    test('should return null for invalid JSON', () => {
      const header = PacketHeader.builder().command(Command.REQUEST).build();
      const body = new TextEncoder().encode('not json').buffer;
      const packet = new Packet(header, body);
      expect(packet.getBodyAsJson()).toBeNull();
    });
  });

  describe('isSuccess', () => {
    test('should return true when header isSuccess', () => {
      const header = PacketHeader.builder()
        .command(Command.RESPONSE)
        .errorCode(0)
        .build();
      const packet = new Packet(header, null);
      expect(packet.isSuccess).toBe(true);
    });
  });
});