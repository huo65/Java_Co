package cn.huo.ohmqttserver.optimization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 回归模型封装
 * 包含模型系数和元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegressionModel {

    /**
     * 回归系数 [intercept, cpuCoef, memCoef, powerCoef, storageCoef, latencyCoef]
     */
    private double[] coefficients;

    /**
     * 模型训练时间
     */
    private LocalDateTime trainedAt;

    /**
     * 训练样本数量
     */
    private int sampleCount;

    /**
     * 模型R²得分
     */
    private double rSquared;

    /**
     * 均方根误差
     */
    private double rmse;

    /**
     * 模型版本（用于缓存）
     */
    private String version;

    /**
     * 预测任务执行时间
     *
     * @param cpuUtil       CPU使用率
     * @param memUsage      内存使用率
     * @param powerRemain   剩余电量
     * @param storageRemain 剩余存储
     * @param latency       网络延迟
     * @return 预测执行时间
     */
    public double predict(double cpuUtil, double memUsage, double powerRemain,
                          double storageRemain, double latency) {
        if (coefficients == null || coefficients.length < 6) {
            throw new IllegalStateException("模型尚未训练或系数不完整");
        }

        return coefficients[0] +
               coefficients[1] * (1 - cpuUtil) +
               coefficients[2] * (1 - memUsage) +
               coefficients[3] * powerRemain +
               coefficients[4] * storageRemain +
               coefficients[5] * (1 - latency);
    }

    /**
     * 使用特征数组预测
     *
     * @param features 特征数组 [cpuUsage, memoryUsage, powerRemain, storageRemain, latency]
     * @return 预测执行时间
     */
    public double predict(double[] features) {
        if (features == null || features.length < 5) {
            throw new IllegalArgumentException("特征数组必须包含至少5个元素");
        }
        return predict(features[0], features[1], features[2], features[3], features[4]);
    }

    /**
     * 检查模型是否有效
     *
     * @return 模型是否有效
     */
    public boolean isValid() {
        return coefficients != null &&
               coefficients.length >= 6 &&
               rSquared > 0.3 && // R²至少大于0.3
               !Double.isNaN(rSquared) &&
               !Double.isInfinite(rSquared);
    }
}
