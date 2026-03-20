package cn.huo.ohmqttserver.optimization.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 决策策略工厂
 * 用于根据配置创建对应的决策策略
 */
@Component
public class DecisionStrategyFactory {

    private final Map<String, DecisionStrategy> strategyMap = new HashMap<>();

    @Autowired
    public DecisionStrategyFactory(List<DecisionStrategy> strategies) {
        for (DecisionStrategy strategy : strategies) {
            strategyMap.put(strategy.getStrategyName(), strategy);
        }
    }

    /**
     * 根据策略名称获取决策策略
     *
     * @param strategyName 策略名称
     * @return 决策策略实例
     */
    public DecisionStrategy getStrategy(String strategyName) {
        DecisionStrategy strategy = strategyMap.get(strategyName);
        if (strategy == null) {
            // 默认返回加权求和策略
            return strategyMap.getOrDefault("weighted-sum", new WeightedSumStrategy());
        }
        return strategy;
    }

    /**
     * 获取加权求和策略（带自定义权重）
     *
     * @param weights 权重数组
     * @return 加权求和策略实例
     */
    public DecisionStrategy createWeightedSumStrategy(double[] weights) {
        return new WeightedSumStrategy(weights);
    }
}
