package cn.huo.ohmqttserver.optimization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 节点选择结果
 * 用于缓存节点选择结果，避免重复计算
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeSelectionResult {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 被选中的节点索引
     */
    private int selectedNodeIndex;

    /**
     * 选中节点的负载评分
     */
    private double loadScore;

    /**
     * 选中节点的特征数组
     */
    private double[] nodeFeatures;

    /**
     * 获取节点唯一标识（用于负载均衡计算）
     */
    public String getNodeIdentifier() {
        if (nodeFeatures == null) {
            return "unknown";
        }
        // 使用特征哈希作为节点标识
        return String.format("node_%.4f_%.4f_%.4f",
                nodeFeatures[0], nodeFeatures[1], nodeFeatures[2]);
    }
}
