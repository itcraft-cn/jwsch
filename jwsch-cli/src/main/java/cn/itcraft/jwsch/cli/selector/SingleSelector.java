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

    @Override
    public InetSocketAddress select(List<InetSocketAddress> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        return candidates.get(0);
    }

    @Override
    public void onConnectSuccess(InetSocketAddress address) {
    }

    @Override
    public void onConnectFailed(InetSocketAddress address) {
    }
}