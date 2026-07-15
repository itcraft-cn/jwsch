export const MAGIC = [0xe7, 0x34];

export const FIXED_HEADER_LENGTH = 27;

export const MAX_TOPIC_LENGTH = 256;

export const MAX_BODY_LENGTH = 10 * 1024 * 1024;

export const Command = {
  REQUEST: 0x01,
  RESPONSE: 0x02,
  PUSH: 0x03,
  BROADCAST: 0x04,
  SUBSCRIBE: 0x05,
  HEARTBEAT: 0x06,
  ACK: 0x07,
  CONNECT_RESPONSE: 0x08,
  CLUSTER_SYNC: 0x10,
  CLUSTER_FORWARD: 0x11,
  CLUSTER_BROADCAST: 0x12
};

export function isValidCommand(command) {
  return (command >= Command.REQUEST && command <= Command.CONNECT_RESPONSE)
    || (command >= Command.CLUSTER_SYNC && command <= Command.CLUSTER_BROADCAST);
}

export const ErrorCode = {
  SUCCESS: 0,
  INVALID_MAGIC: 1,
  INVALID_HEADER_LENGTH: 2,
  INVALID_BODY_LENGTH: 3,
  INVALID_COMMAND: 4,
  INVALID_TOPIC_LENGTH: 5,
  CONNECTION_CLOSED: 100,
  TIMEOUT: 101,
  UNKNOWN: 9999
};