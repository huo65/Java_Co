package cn.huo.ohmqttserver.optimization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 回归模型封装
 * 包含模型系数和元数据
 * 适配整数百分比格式（0-100）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegressionModel {

    /**
     * 百分比转换因子：将0-100整数转换为0-1小数
     */
    public static final double PERCENTAGE_SCALE = 100.0;

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
     * 适配整数百分比格式（0-100）
     *
     * @param cpuUtil       CPU使用率 (0-100)
     * @param memUsage      内存使用率 (0-100)
     * @param powerRemain   剩余电量 (0-100)
     * @param storageRemain 剩余存储 (0-100)
     * @param latency       网络延迟 (0-100)
     * @return 预测执行时间
     */
    public double predict(double cpuUtil, double memUsage, double powerRemain,
                          double storageRemain, double latency) {
        if (coefficients == null || coefficients.length < 6) {
            throw new IllegalStateException("模型尚未训练或系数不完整");
        }

        // 将整数百分比转换为小数进行计算
        double cpuUtilNorm = cpuUtil / PERCENTAGE_SCALE;
        double memUsageNorm = memUsage / PERCENTAGE_SCALE;
        double powerRemainNorm = powerRemain / PERCENTAGE_SCALE;
        double storageRemainNorm = storageRemain / PERCENTAGE_SCALE;
        double latencyNorm = latency / PERCENTAGE_SCALE;

        return coefficients[0] +
               coefficients[1] * (1 - cpuUtilNorm) +
               coefficients[2] * (1 - memUsageNorm) +
               coefficients[3] * powerRemainNorm +
               coefficients[4] * storageRemainNorm +
               coefficients[5] * (1 - latencyNorm);
    }

    /**
     * 使用特征数组预测
     * 适配整数百分比格式（0-100）
     *
     * @param features 特征数组 [cpuUsage(0-100), memoryUsage(0-100), powerRemain(0-100), storageRemain(0-100), latency(0-100)]
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
