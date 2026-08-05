package cn.itcraft.jwsch.cli.selector;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Single node selector.
 *
 * <p>Always selects the first address in the list.
 * Used for single-node deployments or when cluster is not needed.
 */
public class SingleSelector implements NodeSelector {

    /**
     * Always selects the first address in the list.
     *
     * @param candidates list of candidate addresses
     * @return first address, or null if candidates is empty
     */
    @Override
    public InetSocketAddress select(List<InetSocketAddress> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        return candidates.get(0);
    }

    /**
     * No-op for single selector.
     *
     * @param address the address that succeeded
     */
    @Override
    public void onConnectSuccess(InetSocketAddress address) {
    }

    /**
     * No-op for single selector.
     *
     * @param address the address that failed
     */
    @Override
    public void onConnectFailed(InetSocketAddress address) {
    }
}