package cn.itcraft.jwsch.srv.cluster;

public interface NodeChangeListener {
    
    void onNodeJoin(NodeInfo node);
    
    void onNodeLeave(String nodeId);
}