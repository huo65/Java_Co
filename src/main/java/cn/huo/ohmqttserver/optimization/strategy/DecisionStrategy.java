package cn.huo.ohmqttserver.optimization.strategy;

import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;

/**
 * 帕累托最优解决策策略接口
 * 用于从非支配解集中选择最佳解
 */
public interface DecisionStrategy {

    /**
     * 从非支配解集中选择最佳解
     *
     * @param population 非支配解集
     * @return 最佳解
     */
    Solution selectBest(NondominatedPopulation population);

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    String getStrategyName();
}
