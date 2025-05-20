package cn.huo.ohmqttserver.optimization;

import jakarta.persistence.Entity;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
public class NodeInfo {
    // 设备产品名称
    private String deviceName;
    // 电池电量百分比
    private double batteryLevel;
    // 内存占用
    private double memoryUsage;
    // CPU 占用率
    private double cpuUsage;
    // 剩余存储空间
    private double storageFree;

    // 静态集合，用于维护所有 NodeInfo 实例，以 deviceName 为键
    private static final Map<String, NodeInfo> nodeInfoMap = new HashMap<>();

    public static void addNodeInfo(String deviceName, double batteryLevel, double memoryUsage,
                    double cpuUsage, double storageFree, double score) {
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.deviceName = deviceName;
        nodeInfo.batteryLevel = batteryLevel;
        nodeInfo.memoryUsage = memoryUsage;
        nodeInfo.cpuUsage = cpuUsage;
        nodeInfo.storageFree = storageFree;
        nodeInfoMap.put(deviceName, nodeInfo);
    }


    // 更新指定 deviceName 的 NodeInfo 信息
    public static void updateNodeInfo(String deviceName, double batteryLevel, double memoryUsage,
                                      double cpuUsage, double storageFree, double score) {
        NodeInfo nodeInfo = nodeInfoMap.get(deviceName);
        if (nodeInfo != null) {
            nodeInfo.batteryLevel = batteryLevel;
            nodeInfo.memoryUsage = memoryUsage;
            nodeInfo.cpuUsage = cpuUsage;
            nodeInfo.storageFree = storageFree;
        } else {
            addNodeInfo(deviceName, batteryLevel, memoryUsage, cpuUsage, storageFree, score);
        }
    }

    // 根据 deviceName 获取 NodeInfo 对象
    public static NodeInfo getNodeInfo(String deviceName) {
        return nodeInfoMap.get(deviceName);
    }

    // 获取所有节点信息
    public static Map<String, NodeInfo> getAllNodeInfos() {
        return new HashMap<>(nodeInfoMap);
    }
}