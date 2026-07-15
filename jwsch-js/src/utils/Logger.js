const LogLevel = {
  DEBUG: 0,
  INFO: 1,
  WARN: 2,
  ERROR: 3,
  NONE: 4
};

class Logger {
  constructor(prefix = 'Jwsch', level = LogLevel.INFO) {
    this._prefix = prefix;
    this._level = level;
  }

  setLevel(level) {
    this._level = level;
  }

  debug(...args) {
    if (this._level <= LogLevel.DEBUG) {
      this._log('DEBUG', ...args);
    }
  }

  info(...args) {
    if (this._level <= LogLevel.INFO) {
      this._log('INFO', ...args);
    }
  }

  warn(...args) {
    if (this._level <= LogLevel.WARN) {
      this._log('WARN', ...args);
    }
  }

  error(...args) {
    if (this._level <= LogLevel.ERROR) {
      this._log('ERROR', ...args);
    }
  }

  _log(level, ...args) {
    const timestamp = new Date().toISOString();
    const message = `[${timestamp}] [${this._prefix}] [${level}]`;
    switch (level) {
      case 'DEBUG':
      case 'INFO':
        console.log(message, ...args);
        break;
      case 'WARN':
        console.warn(message, ...args);
        break;
      case 'ERROR':
        console.error(message, ...args);
        break;
      default:
        console.log(message, ...args);
    }
  }
}

export { Logger, LogLevel };
export default new Logger();