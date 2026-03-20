package cn.huo.ohmqttserver.optimization;

import cn.huo.ohmqttserver.optimization.config.OptimizationConfig;
import cn.huo.ohmqttserver.optimization.dto.OptimizationSample;
import cn.huo.ohmqttserver.optimization.strategy.DecisionStrategy;
import cn.huo.ohmqttserver.optimization.strategy.DecisionStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.moeaframework.algorithm.NSGAII;
import org.moeaframework.core.*;
import org.moeaframework.core.operator.CompoundVariation;
import org.moeaframework.core.operator.TournamentSelection;
import org.moeaframework.core.operator.real.PM;
import org.moeaframework.core.operator.real.SBX;
import org.moeaframework.core.variable.EncodingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 参数优化器服务
 * 基于NSGA-II算法的多目标优化实现
 * 优化版本：支持Spring依赖注入、配置化、策略模式
 * @author huozj
 */
@Slf4j
@Service
public class OmegaOptimizer {

    private final TaskModelEvaluator evaluator;
    private final OptimizationConfig config;
    private final DecisionStrategyFactory strategyFactory;

    @Autowired
    public OmegaOptimizer(TaskModelEvaluator evaluator,
                          OptimizationConfig config,
                          DecisionStrategyFactory strategyFactory) {
        this.evaluator = evaluator;
        this.config = config;
        this.strategyFactory = strategyFactory;
    }

    /**
     * 优化权重参数
     * @param samples 任务样本列表
     * @return 优化后的权重参数数组 [w1, w2, w3, w4, w5]
     */
    public double[] optimize(List<OptimizationSample> samples) {
        if (samples == null || samples.size() < config.getMinSampleSize()) {
            log.warn("样本数量不足，需要至少{}个样本，当前: {}",
                    config.getMinSampleSize(),
                    samples == null ? 0 : samples.size());
            return getDefaultOmega();
        }

        log.info("开始优化，样本数: {}", samples.size());
        long startTime = System.currentTimeMillis();

        // 设置样本并训练模型
        evaluator.setSamples(samples);
        evaluator.trainModel();

        // 执行NSGA-II优化
        Algorithm nsga2 = createAlgorithm();

        // 运行算法
        int maxEvaluations = config.getMaxEvaluations();
        int evaluations = 0;
        while (evaluations < maxEvaluations) {
            nsga2.step();
            evaluations++;
        }

        NondominatedPopulation result = nsga2.getResult();

        // 使用决策策略选择最佳解
        DecisionStrategy strategy = strategyFactory.getStrategy(config.getDecisionStrategy());
        if (config.getObjectiveWeights() != null && config.getObjectiveWeights().length == 3) {
            strategy = strategyFactory.createWeightedSumStrategy(config.getObjectiveWeights());
        }

        Solution best = strategy.selectBest(result);

        // 提取权重参数
        double[] omega = extractOmegaFromSolution(best);

        long duration = System.currentTimeMillis() - startTime;
        log.info("优化完成，耗时: {}ms, 结果: [{}, {}, {}, {}, {}]",
                duration,
                String.format("%.4f", omega[0]),
                String.format("%.4f", omega[1]),
                String.format("%.4f", omega[2]),
                String.format("%.4f", omega[3]),
                String.format("%.4f", omega[4]));

        return omega;
    }

    /**
     * 从解决方案中提取omega参数
     */
    private double[] extractOmegaFromSolution(Solution best) {
        double[] omega = new double[5];

        if (best != null) {
            for (int i = 0; i < 5; i++) {
                omega[i] = EncodingUtils.getReal(best.getVariable(i));
            }
        }

        // 归一化，确保权重和为1
        double sum = Arrays.stream(omega).sum();
        if (sum > 0) {
            for (int i = 0; i < 5; i++) {
                omega[i] /= sum;
            }
        }

        return omega;
    }

    /**
     * 创建NSGA-II算法实例
     * @return NSGA-II算法实例
     */
    private Algorithm createAlgorithm() {
        Problem problem = new OmegaOptimizationProblem(evaluator, config);

        // 变异操作：模拟二进制交叉 + 多项式变异
        Variation variation = new CompoundVariation(
                new SBX(config.getCrossoverProbability(), config.getCrossoverDistributionIndex()),
                new PM(config.getMutationProbability(), config.getMutationDistributionIndex())
        );

        // 初始化NSGA-II算法
        return new NSGAII(
                problem,
                new NondominatedSortingPopulation(),
                null,
                new TournamentSelection(config.getTournamentSize()),
                variation,
                () -> initializePopulation(problem)
        );
    }

    /**
     * 初始化种群
     */
    private Solution[] initializePopulation(Problem problem) {
        Solution[] pop = new Solution[config.getPopulationSize()];
        for (int i = 0; i < config.getPopulationSize(); i++) {
            pop[i] = problem.newSolution();
        }
        return pop;
    }

    /**
     * 获取默认omega参数
     */
    private double[] getDefaultOmega() {
        return new double[]{0.2, 0.2, 0.2, 0.2, 0.2};
    }

    /**
     * 获取优化配置
     */
    public OptimizationConfig getConfig() {
        return config;
    }
}
