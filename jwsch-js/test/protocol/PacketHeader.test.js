import { PacketHeader } from '../../src/protocol/PacketHeader.js';
import { Command, FIXED_HEADER_LENGTH } from '../../src/protocol/constants.js';

describe('PacketHeader', () => {
  describe('constructor', () => {
    test('should create header with default values', () => {
      const header = new PacketHeader({ command: Command.REQUEST });
      expect(header.command).toBe(Command.REQUEST);
      expect(header.errorCode).toBe(0);
      expect(header.sourceId).toBe(0);
      expect(header.targetId).toBe(0);
      expect(header.topic).toBeNull();
      expect(header.bodyLength).toBe(0);
    });

    test('should create header with custom values', () => {
      const header = new PacketHeader({
        command: Command.REQUEST,
        errorCode: 0,
        sourceId: 12345,
        targetId: 67890,
        topic: '/api/test',
        bodyLength: 100
      });
      expect(header.command).toBe(Command.REQUEST);
      expect(header.errorCode).toBe(0);
      expect(header.sourceId).toBe(12345);
      expect(header.targetId).toBe(67890);
      expect(header.topic).toBe('/api/test');
      expect(header.bodyLength).toBe(100);
    });

    test('should calculate headerLength correctly', () => {
      const header = new PacketHeader({
        command: Command.REQUEST,
        topic: '/api/test'
      });
      expect(header.headerLength).toBe(FIXED_HEADER_LENGTH + 9);
    });

    test('should throw error for invalid command', () => {
      expect(() => {
        new PacketHeader({ command: 0xFF });
      }).toThrow('Invalid command');
    });

    test('should throw error for topic too long', () => {
      const longTopic = 'a'.repeat(300);
      expect(() => {
        new PacketHeader({ command: Command.REQUEST, topic: longTopic });
      }).toThrow('Topic length exceeds max');
    });
  });

  describe('isSuccess', () => {
    test('should return true when errorCode is 0', () => {
      const header = new PacketHeader({ command: Command.RESPONSE, errorCode: 0 });
      expect(header.isSuccess).toBe(true);
    });

    test('should return false when errorCode is non-zero', () => {
      const header = new PacketHeader({ command: Command.RESPONSE, errorCode: 100 });
      expect(header.isSuccess).toBe(false);
    });
  });

  describe('Builder', () => {
    test('should build header using builder pattern', () => {
      const header = PacketHeader.builder()
        .command(Command.REQUEST)
        .errorCode(0)
        .sourceId(12345)
        .targetId(67890)
        .topic('/api/test')
        .bodyLength(100)
        .build();

      expect(header.command).toBe(Command.REQUEST);
      expect(header.sourceId).toBe(12345);
      expect(header.targetId).toBe(67890);
      expect(header.topic).toBe('/api/test');
      expect(header.bodyLength).toBe(100);
    });
  });
});