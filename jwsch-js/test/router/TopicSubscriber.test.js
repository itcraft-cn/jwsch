import { TopicSubscriber } from '../../src/router/TopicSubscriber.js';

describe('TopicSubscriber', () => {
  let subscriber;

  beforeEach(() => {
    subscriber = new TopicSubscriber();
  });

  describe('subscribe', () => {
    test('should subscribe to topic', () => {
      const callback = jest.fn();
      const result = subscriber.subscribe('/topic/news', callback);
      
      expect(result).toBe(true);
      expect(subscriber.has('/topic/news')).toBe(true);
    });

    test('should support multiple callbacks per topic', () => {
      const cb1 = jest.fn();
      const cb2 = jest.fn();
      
      subscriber.subscribe('/topic/news', cb1);
      subscriber.subscribe('/topic/news', cb2);
      
      subscriber.notify('/topic/news', { data: 'test' });
      
      expect(cb1).toHaveBeenCalled();
      expect(cb2).toHaveBeenCalled();
    });
  });

  describe('unsubscribe', () => {
    test('should unsubscribe from topic', () => {
      const callback = jest.fn();
      subscriber.subscribe('/topic/news', callback);
      
      const result = subscriber.unsubscribe('/topic/news', callback);
      
      expect(result).toBe(true);
      expect(subscriber.has('/topic/news')).toBe(false);
    });

    test('should unsubscribe all callbacks for topic', () => {
      subscriber.subscribe('/topic/news', jest.fn());
      subscriber.subscribe('/topic/news', jest.fn());
      
      subscriber.unsubscribe('/topic/news');
      
      expect(subscriber.has('/topic/news')).toBe(false);
    });
  });

  describe('notify', () => {
    test('should notify all subscribers', () => {
      const cb1 = jest.fn();
      const cb2 = jest.fn();
      
      subscriber.subscribe('/topic/news', cb1);
      subscriber.subscribe('/topic/news', cb2);
      
      const packet = { data: 'test' };
      const count = subscriber.notify('/topic/news', packet);
      
      expect(count).toBe(2);
      expect(cb1).toHaveBeenCalledWith(packet);
      expect(cb2).toHaveBeenCalledWith(packet);
    });

    test('should return 0 for unknown topic', () => {
      const count = subscriber.notify('/topic/unknown', {});
      expect(count).toBe(0);
    });

    test('should catch callback errors', () => {
      const cb1 = jest.fn(() => { throw new Error('test'); });
      const cb2 = jest.fn();
      
      subscriber.subscribe('/topic/news', cb1);
      subscriber.subscribe('/topic/news', cb2);
      
      const count = subscriber.notify('/topic/news', {});
      
      expect(count).toBe(2);
      expect(cb2).toHaveBeenCalled();
    });
  });

  describe('getTopics', () => {
    test('should return all topics', () => {
      subscriber.subscribe('/topic/news', jest.fn());
      subscriber.subscribe('/topic/alerts', jest.fn());
      
      const topics = subscriber.getTopics();
      
      expect(topics).toContain('/topic/news');
      expect(topics).toContain('/topic/alerts');
    });
  });

  describe('clear', () => {
    test('should clear all subscriptions', () => {
      subscriber.subscribe('/topic/news', jest.fn());
      subscriber.subscribe('/topic/alerts', jest.fn());
      
      subscriber.clear();
      
      expect(subscriber.size).toBe(0);
    });
  });
});