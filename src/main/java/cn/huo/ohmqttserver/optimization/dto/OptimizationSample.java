package cn.huo.ohmqttserver.optimization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 优化专用的轻量级任务样本DTO
 * 用于减少JPA实体在优化过程中的内存开销
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationSample {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 候选节点特征列表 [cpuUsage, memoryUsage, powerRemain, storageRemain, latency]
     */
    private List<double[]> candidateNodeFeatures;

    /**
     * 被选中的节点索引
     */
    private int chosenNodeIndex;

    /**
     * 任务执行时长
     */
    private double duration;

    /**
     * 计算负载评分
     *
     * @param nodeFeatures 节点特征数组
     * @param omega        权重参数
     * @return 负载评分
     */
    public static double calculateLoadScore(double[] nodeFeatures, double[] omega) {
        if (nodeFeatures == null || nodeFeatures.length < 5 || omega == null || omega.length < 5) {
            return 0.0;
        }
        // 公式: S = w1*(1-CPU) + w2*(1-MEM) + w3*POWER + w4*STORAGE + w5*(1-LATENCY)
        return omega[0] * (1 - nodeFeatures[0]) +
               omega[1] * (1 - nodeFeatures[1]) +
               omega[2] * nodeFeatures[2] +
               omega[3] * nodeFeatures[3] +
               omega[4] * (1 - nodeFeatures[4]);
    }

    /**
     * 获取被选中的节点特征
     *
     * @return 被选中节点的特征数组
     */
    public double[] getChosenNodeFeatures() {
        if (candidateNodeFeatures == null || chosenNodeIndex < 0 || chosenNodeIndex >= candidateNodeFeatures.size()) {
            return null;
        }
        return candidateNodeFeatures.get(chosenNodeIndex);
    }
}
