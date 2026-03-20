package cn.huo.ohmqttserver.optimization.strategy;

import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.springframework.stereotype.Component;

/**
 * 理想点距离决策策略
 * 选择距离理想点（各目标最优值）最近的解
 */
@Component
public class IdealPointStrategy implements DecisionStrategy {

    @Override
    public Solution selectBest(NondominatedPopulation population) {
        if (population == null || population.isEmpty()) {
            return null;
        }

        // 计算理想点
        double[] idealPoint = calculateIdealPoint(population);

        Solution best = null;
        double minDistance = Double.MAX_VALUE;

        for (Solution sol : population) {
            double distance = calculateEuclideanDistance(sol, idealPoint);
            if (distance < minDistance) {
                minDistance = distance;
                best = sol;
            }
        }

        return best;
    }

    /**
     * 计算理想点（各目标的最优值）
     */
    private double[] calculateIdealPoint(NondominatedPopulation population) {
        double[] ideal = new double[]{Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};

        for (Solution sol : population) {
            for (int i = 0; i < 3; i++) {
                if (sol.getObjective(i) < ideal[i]) {
                    ideal[i] = sol.getObjective(i);
                }
            }
        }

        return ideal;
    }

    /**
     * 计算解与理想点的欧氏距离
     */
    private double calculateEuclideanDistance(Solution sol, double[] idealPoint) {
        double sum = 0.0;
        for (int i = 0; i < 3; i++) {
            double diff = sol.getObjective(i) - idealPoint[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    @Override
    public String getStrategyName() {
        return "ideal-point";
    }
}
