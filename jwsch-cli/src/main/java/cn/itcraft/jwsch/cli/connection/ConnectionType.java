/**
 * Connection type enumeration.
 *
 * <p>Distinguishes between connection roles:
 * <ul>
 *   <li>FRONTEND: Client-facing connections (receiving requests)</li>
 *   <li>BACKEND: Server-facing connections (sending requests)</li>
 * </ul>
 *
 * <p>Used for connection routing and management policies.
 */
public enum ConnectionType {
    FRONTEND,
    BACKEND
}