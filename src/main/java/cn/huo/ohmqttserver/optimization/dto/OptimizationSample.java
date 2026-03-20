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
     * 百分比转换因子：将0-100整数转换为0-1小数
     */
    public static final double PERCENTAGE_SCALE = 100.0;

    /**
     * 计算负载评分
     * 适配整数百分比格式（0-100）
     *
     * @param nodeFeatures 节点特征数组 [cpuUsage(0-100), memoryUsage(0-100), powerRemain(0-100), storageRemain(0-100), latency(0-100)]
     * @param omega        权重参数
     * @return 负载评分
     */
    public static double calculateLoadScore(double[] nodeFeatures, double[] omega) {
        if (nodeFeatures == null || nodeFeatures.length < 5 || omega == null || omega.length < 5) {
            return 0.0;
        }
        // 将整数百分比转换为小数进行计算
        double cpuUtil = nodeFeatures[0] / PERCENTAGE_SCALE;
        double memUsage = nodeFeatures[1] / PERCENTAGE_SCALE;
        double powerRemain = nodeFeatures[2] / PERCENTAGE_SCALE;
        double storageRemain = nodeFeatures[3] / PERCENTAGE_SCALE;
        double latency = nodeFeatures[4] / PERCENTAGE_SCALE;

        // 公式: S = w1*(1-CPU) + w2*(1-MEM) + w3*POWER + w4*STORAGE + w5*(1-LATENCY)
        return omega[0] * (1 - cpuUtil) +
               omega[1] * (1 - memUsage) +
               omega[2] * powerRemain +
               omega[3] * storageRemain +
               omega[4] * (1 - latency);
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
