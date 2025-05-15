package cn.huo.ohmqttserver.optimization;

import java.util.List;

public class TaskModelEvaluator {

    private final List<TaskSample> samples;

    public TaskModelEvaluator(List<TaskSample> samples) {
        this.samples = samples;
    }

    // 预测：load 越小 -> duration 越短（可更复杂地拟合）
    public double evaluateDurationWithOmega(double[] omega) {
        return samples.stream()
            .mapToDouble(sample -> {
                double load = sample.computeLoad(omega);
                return Math.abs(sample.duration - load); // 简化目标函数
            }).average().orElse(Double.MAX_VALUE);
    }

    // 附加目标：资源使用波动越小越好（提高系统稳定性）
    public double evaluateLoadVariance(double[] omega) {
        double mean = samples.stream().mapToDouble(s -> s.computeLoad(omega)).average().orElse(0);
        return samples.stream()
                .mapToDouble(s -> Math.pow(s.computeLoad(omega) - mean, 2))
                .average().orElse(0);
    }
}
