package cn.huo.ohmqttserver.optimization.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 优化模块配置类
 * 集中管理NSGA-II算法及相关参数配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "optimization.nsga2")
public class OptimizationConfig {

    /**
     * 种群大小
     */
    private int populationSize = 100;

    /**
     * 最大评估次数
     */
    private int maxEvaluations = 1000;

    /**
     * 交叉概率
     */
    private double crossoverProbability = 1.0;

    /**
     * 交叉分布指数
     */
    private double crossoverDistributionIndex = 15.0;

    /**
     * 变异概率 (默认 1/变量数 = 0.2)
     */
    private double mutationProbability = 0.2;

    /**
     * 变异分布指数
     */
    private double mutationDistributionIndex = 0.1;

    /**
     * 锦标赛选择大小
     */
    private int tournamentSize = 2;

    /**
     * 决策策略类型: weighted-sum, ideal-point, fuzzy
     */
    private String decisionStrategy = "weighted-sum";

    /**
     * 加权求和策略的权重 [任务延迟, 负载均衡, 能耗]
     */
    private double[] objectiveWeights = {0.5, 0.3, 0.2};

    /**
     * 最小样本数量要求
     */
    private int minSampleSize = 10;

    /**
     * 是否启用模型缓存
     */
    private boolean enableModelCache = true;

    /**
     * 模型缓存过期时间(分钟)
     */
    private int modelCacheExpireMinutes = 30;

    /**
     * 是否启用节点选择结果缓存
     */
    private boolean enableSelectionCache = true;

    /**
     * 收敛阈值
     */
    private double convergenceThreshold = 1e-6;

    /**
     * 最小迭代代数
     */
    private int minGenerations = 50;
}
