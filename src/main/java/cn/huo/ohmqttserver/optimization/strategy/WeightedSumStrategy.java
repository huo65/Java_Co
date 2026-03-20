package cn.huo.ohmqttserver.optimization.strategy;

import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.springframework.stereotype.Component;

/**
 * 加权求和决策策略
 * 通过加权求和目标值选择最佳解
 */
@Component
public class WeightedSumStrategy implements DecisionStrategy {

    private final double[] weights;

    public WeightedSumStrategy() {
        // 默认权重: 任务延迟 0.5, 负载均衡 0.3, 能耗 0.2
        this.weights = new double[]{0.5, 0.3, 0.2};
    }

    public WeightedSumStrategy(double[] weights) {
        if (weights == null || weights.length != 3) {
            throw new IllegalArgumentException("权重数组必须包含3个元素");
        }
        this.weights = weights.clone();
    }

    @Override
    public Solution selectBest(NondominatedPopulation population) {
        if (population == null || population.isEmpty()) {
            return null;
        }

        Solution best = null;
        double bestScore = Double.MAX_VALUE;

        for (Solution sol : population) {
            // 计算加权目标值（所有目标都越小越好）
            double score = sol.getObjective(0) * weights[0] +
                          sol.getObjective(1) * weights[1] +
                          sol.getObjective(2) * weights[2];

            if (score < bestScore) {
                bestScore = score;
                best = sol;
            }
        }

        return best;
    }

    @Override
    public String getStrategyName() {
        return "weighted-sum";
    }
}
