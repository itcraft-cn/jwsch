export class IdGenerator {
  constructor(workerId = 1, datacenterId = 1) {
    this._workerId = workerId;
    this._datacenterId = datacenterId;
    this._sequence = 0;
    this._lastTimestamp = 0;
  }

  nextId() {
    const timestamp = Date.now();

    if (timestamp === this._lastTimestamp) {
      this._sequence++;
    } else {
      this._sequence = 0;
      this._lastTimestamp = timestamp;
    }

    return BigInt(timestamp) * 1000000n
      + BigInt(this._datacenterId) * 100000n
      + BigInt(this._workerId) * 10000n
      + BigInt(this._sequence);
  }
}