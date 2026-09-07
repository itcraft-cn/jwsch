package cn.itcraft.jwsch.cli.connection;

/**
 * Connection status enumeration.
 *
 * <p>Represents lifecycle states of network connections:
 * <ul>
 *   <li>ACTIVE: Currently transmitting data</li>
 *   <li>IDLE: Connected but inactive</li>
 *   <li>CLOSED: Connection terminated</li>
 * </ul>
 *
 * <p>Used by ConnectionInfo to track connection health.
 */
public enum ConnectionStatus {
    ACTIVE,
    IDLE,
    CLOSED
}