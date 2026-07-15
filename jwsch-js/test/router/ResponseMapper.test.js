import { ResponseMapper } from '../../src/router/ResponseMapper.js';

describe('ResponseMapper', () => {
  let mapper;

  beforeEach(() => {
    mapper = new ResponseMapper(1000);
  });

  describe('register', () => {
    test('should register request and return promise', () => {
      const promise = mapper.register(12345);
      expect(promise).toBeInstanceOf(Promise);
      expect(mapper.has(12345)).toBe(true);
    });

    test('should reject on timeout', async () => {
      await expect(mapper.register(12345, 50)).rejects.toThrow('Request timeout');
      expect(mapper.has(12345)).toBe(false);
    });
  });

  describe('resolve', () => {
    test('should resolve registered request', async () => {
      const promise = mapper.register(12345);
      const packet = { data: 'test' };

      const resolved = mapper.resolve(12345, packet);
      expect(resolved).toBe(true);

      const result = await promise;
      expect(result).toBe(packet);
    });

    test('should return false for unknown request', () => {
      const resolved = mapper.resolve(99999, {});
      expect(resolved).toBe(false);
    });
  });

  describe('reject', () => {
    test('should reject registered request', async () => {
      const promise = mapper.register(12345);
      const error = new Error('test error');

      const rejected = mapper.reject(12345, error);
      expect(rejected).toBe(true);

      await expect(promise).rejects.toThrow('test error');
    });
  });

  describe('cancel', () => {
    test('should cancel registered request', () => {
      mapper.register(12345);
      mapper.cancel(12345);
      expect(mapper.has(12345)).toBe(false);
    });
  });

  describe('cancelAll', () => {
    test('should cancel all requests', async () => {
      const p1 = mapper.register(1);
      const p2 = mapper.register(2);

      mapper.cancelAll(new Error('cancelled'));

      expect(mapper.size).toBe(0);
      await expect(p1).rejects.toThrow('cancelled');
      await expect(p2).rejects.toThrow('cancelled');
    });
  });

  describe('size', () => {
    test('should return number of pending requests', () => {
      expect(mapper.size).toBe(0);
      mapper.register(1);
      expect(mapper.size).toBe(1);
      mapper.register(2);
      expect(mapper.size).toBe(2);
    });
  });
});