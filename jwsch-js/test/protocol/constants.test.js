import {
  MAGIC,
  FIXED_HEADER_LENGTH,
  MAX_TOPIC_LENGTH,
  MAX_BODY_LENGTH,
  Command,
  ErrorCode,
  isValidCommand
} from '../../src/protocol/constants.js';

describe('constants', () => {
  describe('MAGIC', () => {
    test('should have correct magic bytes', () => {
      expect(MAGIC).toEqual([0xe7, 0x34]);
    });
  });

  describe('FIXED_HEADER_LENGTH', () => {
    test('should be 27', () => {
      expect(FIXED_HEADER_LENGTH).toBe(27);
    });
  });

  describe('MAX_TOPIC_LENGTH', () => {
    test('should be 256', () => {
      expect(MAX_TOPIC_LENGTH).toBe(256);
    });
  });

  describe('MAX_BODY_LENGTH', () => {
    test('should be 10MB', () => {
      expect(MAX_BODY_LENGTH).toBe(10 * 1024 * 1024);
    });
  });

  describe('Command', () => {
    test('should have REQUEST command', () => {
      expect(Command.REQUEST).toBe(0x01);
    });

    test('should have RESPONSE command', () => {
      expect(Command.RESPONSE).toBe(0x02);
    });

    test('should have PUSH command', () => {
      expect(Command.PUSH).toBe(0x03);
    });

    test('should have BROADCAST command', () => {
      expect(Command.BROADCAST).toBe(0x04);
    });

    test('should have SUBSCRIBE command', () => {
      expect(Command.SUBSCRIBE).toBe(0x05);
    });

    test('should have HEARTBEAT command', () => {
      expect(Command.HEARTBEAT).toBe(0x06);
    });

    test('should have ACK command', () => {
      expect(Command.ACK).toBe(0x07);
    });
  });

  describe('ErrorCode', () => {
    test('should have SUCCESS code', () => {
      expect(ErrorCode.SUCCESS).toBe(0);
    });

    test('should have error codes', () => {
      expect(ErrorCode.INVALID_MAGIC).toBe(1);
      expect(ErrorCode.TIMEOUT).toBe(101);
      expect(ErrorCode.UNKNOWN).toBe(9999);
    });
  });

  describe('isValidCommand', () => {
    test('should return true for valid commands', () => {
      expect(isValidCommand(Command.REQUEST)).toBe(true);
      expect(isValidCommand(Command.RESPONSE)).toBe(true);
      expect(isValidCommand(Command.PUSH)).toBe(true);
      expect(isValidCommand(Command.HEARTBEAT)).toBe(true);
      expect(isValidCommand(Command.ACK)).toBe(true);
    });

    test('should return true for cluster commands', () => {
      expect(isValidCommand(Command.CLUSTER_SYNC)).toBe(true);
      expect(isValidCommand(Command.CLUSTER_FORWARD)).toBe(true);
      expect(isValidCommand(Command.CLUSTER_BROADCAST)).toBe(true);
    });

    test('should return false for invalid commands', () => {
      expect(isValidCommand(0x00)).toBe(false);
      expect(isValidCommand(0x09)).toBe(false);
      expect(isValidCommand(0xFF)).toBe(false);
    });
  });
});