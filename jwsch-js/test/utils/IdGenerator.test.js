import { IdGenerator } from '../../src/utils/IdGenerator.js';

describe('IdGenerator', () => {
  describe('nextId', () => {
    test('should generate unique IDs', () => {
      const generator = new IdGenerator();
      const ids = new Set();
      
      for (let i = 0; i < 100; i++) {
        const id = generator.nextId();
        expect(ids.has(id)).toBe(false);
        ids.add(id);
      }
    });

    test('should generate positive BigInts', () => {
      const generator = new IdGenerator();
      
      for (let i = 0; i < 10; i++) {
        const id = generator.nextId();
        expect(typeof id).toBe('bigint');
        expect(id > 0n).toBe(true);
      }
    });

    test('should generate different IDs for different workers', () => {
      const gen1 = new IdGenerator(1, 1);
      const gen2 = new IdGenerator(2, 1);

      const id1 = gen1.nextId();
      const id2 = gen2.nextId();

      expect(id1).not.toBe(id2);
    });
  });
});