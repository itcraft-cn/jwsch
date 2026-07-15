package cn.itcraft.jwsch.cli.selector;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Node selector interface for cluster client connection.
 *
 * <p>Selects which cluster node to connect to from a list of candidates.
 * Implementations provide different selection strategies:
 * <ul>
 *   <li>RandomSelector: Random selection</li>
 *   <li>RoundRobinSelector: Round-robin selection</li>
 *   <li>PrioritySelector: Prefer base-port node</li>
 *   <li>SingleSelector: Always select the first address</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * NodeSelector selector = new RoundRobinSelector();
 * InetSocketAddress address = selector.select(addresses);
 * selector.onConnectSuccess(address);
 * </pre>
 */
public interface NodeSelector {

    /**
     * Select a node address from candidates.
     *
     * @param candidates List of candidate addresses
     * @return Selected address, or null if no candidates available
     */
    InetSocketAddress select(List<InetSocketAddress> candidates);

    /**
     * Called when connection succeeds.
     *
     * <p>Used to track connection state for selection strategy.
     *
     * @param address The address that succeeded
     */
    void onConnectSuccess(InetSocketAddress address);

    /**
     * Called when connection fails.
     *
     * <p>Used to track failures for selection strategy.
     *
     * @param address The address that failed
     */
    void onConnectFailed(InetSocketAddress address);
}