package cn.huo.ohmqttserver.optimization;

import lombok.Data;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
public class NodeInfo {
    // 设备产品名称
    private String deviceName;
    // 电池电量百分比
    private double powerRemain;
    // 内存占用
    private double memoryUsage;
    // CPU 占用率
    private double cpuUsage;
    // 剩余存储空间
    private double storageRemain;

    // 静态集合，用于维护所有 NodeInfo 实例，以 deviceName 为键
    private static final Map<String, NodeInfo> nodeInfoMap = new HashMap<>();

    public static void addNodeInfo(String deviceName, double batteryLevel, double memoryUsage,
                    double cpuUsage, double storageFree, double score) {
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.deviceName = deviceName;
        nodeInfo.powerRemain = batteryLevel;
        nodeInfo.memoryUsage = memoryUsage;
        nodeInfo.cpuUsage = cpuUsage;
        nodeInfo.storageRemain = storageFree;
        nodeInfoMap.put(deviceName, nodeInfo);
    }


    // 更新指定 deviceName 的 NodeInfo 信息
    public static void updateNodeInfo(String deviceName, double batteryLevel, double memoryUsage,
                                      double cpuUsage, double storageFree, double score) {
        NodeInfo nodeInfo = nodeInfoMap.get(deviceName);
        if (nodeInfo != null) {
            nodeInfo.powerRemain = batteryLevel;
            nodeInfo.memoryUsage = memoryUsage;
            nodeInfo.cpuUsage = cpuUsage;
            nodeInfo.storageRemain = storageFree;
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
    public static void parseAndUpdateNodeInfo(String statusMessage) {
        try {
            JSONObject root = new JSONObject(statusMessage);

            // 获取外层 deviceName 和 score
            String deviceName = root.optString("deviceName");
            double score = Double.parseDouble(root.optString("score", "0.0"));

            // 获取 params 对象
            JSONObject params = root.optJSONObject("params");
            if (params == null) {
                System.err.println("Missing 'params' in message.");
                return;
            }

            // 提取各项指标
            double cpuUsage = params.optDouble("cpuUsage", 0.0);
            double memoryUsage = params.optDouble("memoryUsage", 0.0);
            double storageFree = params.optDouble("storageFree", 0.0);
            double batteryLevel = params.optDouble("batteryLevel", 0.0);

            // 调用 NodeInfo 的更新方法
            NodeInfo.updateNodeInfo(deviceName, batteryLevel, memoryUsage, cpuUsage, storageFree, score);

            System.out.println("Updated NodeInfo for device: " + deviceName);

        } catch (Exception e) {
            System.err.println("Error parsing status message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}