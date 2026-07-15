<template>
  <div class="app">
    <header class="header">
      <h1>Jwsch Sample Webapp</h1>
      <div class="status">
        <span :class="connected ? 'connected' : 'disconnected'">
          {{ connected ? 'Connected' : 'Disconnected' }}
        </span>
      </div>
    </header>

    <main class="main">
      <div class="controls">
        <div class="control-group">
          <label>WebSocket URL:</label>
          <input v-model="wsUrl" type="text" placeholder="ws://localhost:8080/ws" />
        </div>
        <div class="control-group">
          <label>Topic:</label>
          <input v-model="topic" type="text" placeholder="/topic/news" />
        </div>
        <div class="button-group">
          <button @click="connect" :disabled="connected">Connect</button>
          <button @click="disconnect" :disabled="!connected">Disconnect</button>
          <button @click="subscribe" :disabled="!connected">Subscribe</button>
          <button @click="clearMessages">Clear</button>
        </div>
      </div>

      <div class="messages">
        <h2>Messages ({{ messages.length }})</h2>
        <div class="message-list">
          <div v-for="(msg, index) in messages" :key="index" class="message">
            <span class="time">{{ formatTime(msg.timestamp) }}</span>
            <span class="topic">[{{ msg.topic }}]</span>
            <span class="content">{{ msg.content }}</span>
          </div>
          <div v-if="messages.length === 0" class="no-messages">
            No messages yet. Connect and subscribe to receive messages.
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import { JwschClient } from './lib/client/JwschClient.js'

export default {
  name: 'App',
  data() {
    return {
      wsUrl: 'ws://localhost:8080/ws',
      topic: '/topic/news',
      connected: false,
      messages: [],
      client: null
    }
  },
  methods: {
    async connect() {
      if (this.client) {
        this.client.disconnect()
      }

      this.client = new JwschClient({
        url: this.wsUrl,
        reconnect: true,
        heartbeatInterval: 30000
      })

      this.client.on('connected', () => {
        this.connected = true
        this.addMessage('system', 'Connected to server')
      })

      this.client.on('disconnected', () => {
        this.connected = false
        this.addMessage('system', 'Disconnected from server')
      })

      this.client.on('error', (err) => {
        this.addMessage('error', 'Error: ' + err)
      })

      try {
        await this.client.connect()
      } catch (err) {
        this.addMessage('error', 'Failed to connect: ' + err.message)
      }
    },

    disconnect() {
      if (this.client) {
        this.client.disconnect()
        this.client = null
        this.connected = false
      }
    },

    async subscribe() {
      if (!this.client || !this.connected) {
        return
      }

      try {
        await this.client.subscribe(this.topic, (packet) => {
          const body = packet.getBodyAsJson()
          if (body) {
            this.addMessage(this.topic, body.content || JSON.stringify(body))
          } else {
            this.addMessage(this.topic, packet.getBodyAsText() || 'empty')
          }
        })
        this.addMessage('system', 'Subscribed to ' + this.topic)
      } catch (err) {
        this.addMessage('error', 'Subscribe failed: ' + err.message)
      }
    },

    addMessage(topic, content) {
      this.messages.push({
        timestamp: Date.now(),
        topic: topic,
        content: content
      })

      if (this.messages.length > 100) {
        this.messages.shift()
      }
    },

    clearMessages() {
      this.messages = []
    },

    formatTime(timestamp) {
      const date = new Date(timestamp)
      return date.toLocaleTimeString()
    }
  },

  beforeUnmount() {
    this.disconnect()
  }
}
</script>

<style>
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: #f5f5f5;
}

.app {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.header h1 {
  margin: 0;
  color: #333;
}

.status span {
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
}

.connected {
  background: #d4edda;
  color: #155724;
}

.disconnected {
  background: #f8d7da;
  color: #721c24;
}

.main {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.controls {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.control-group {
  margin-bottom: 15px;
}

.control-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #333;
}

.control-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.button-group {
  display: flex;
  gap: 10px;
}

button {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  background: #007bff;
  color: #fff;
  transition: background 0.2s;
}

button:hover:not(:disabled) {
  background: #0056b3;
}

button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.messages h2 {
  margin: 0 0 15px 0;
  color: #333;
}

.message-list {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #eee;
  border-radius: 4px;
  padding: 10px;
  background: #fafafa;
}

.message {
  padding: 8px;
  margin-bottom: 8px;
  background: #fff;
  border-radius: 4px;
  border-left: 3px solid #007bff;
}

.message:last-child {
  margin-bottom: 0;
}

.time {
  color: #999;
  font-size: 12px;
  margin-right: 10px;
}

.topic {
  color: #007bff;
  font-weight: 500;
  margin-right: 10px;
}

.content {
  color: #333;
}

.no-messages {
  text-align: center;
  color: #999;
  padding: 20px;
}
</style>