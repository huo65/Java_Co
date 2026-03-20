package cn.huo.ohmqttserver.optimization;

import cn.huo.ohmqttserver.optimization.dto.NodeSelectionResult;
import cn.huo.ohmqttserver.optimization.dto.OptimizationSample;
import cn.huo.ohmqttserver.optimization.dto.RegressionModel;
import cn.huo.ohmqttserver.optimization.training.ModelCache;
import cn.huo.ohmqttserver.optimization.training.ModelTrainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 任务模型评估器，提供评估需要的方法
 * 基于文档要求实现动态优化LoadScore权重参数的评估功能
 * 优化版本：支持缓存、DTO转换、性能优化
 * @author huozj
 */
@Slf4j
@Component
public class TaskModelEvaluator {

    private final ModelTrainer modelTrainer;
    private final ModelCache modelCache;

    private List<OptimizationSample> samples;
    private RegressionModel regressionModel;

    /**
     * 节点选择结果缓存（omega -> 选择结果）
     */
    private final Map<String, List<NodeSelectionResult>> selectionCache = new HashMap<>();

    @Autowired
    public TaskModelEvaluator(ModelTrainer modelTrainer, ModelCache modelCache) {
        this.modelTrainer = modelTrainer;
        this.modelCache = modelCache;
    }

    /**
     * 设置样本数据（用于非Spring管理的调用）
     * @param samples 优化样本列表
     */
    public void setSamples(List<OptimizationSample> samples) {
        this.samples = samples;
        this.selectionCache.clear();
    }

    /**
     * 获取当前样本
     * @return 样本列表
     */
    public List<OptimizationSample> getSamples() {
        return samples;
    }

    /**
     * 训练线性回归模型
     * 优先使用缓存，如果缓存无效则重新训练
     */
    public void trainModel() {
        // 尝试从缓存获取模型
        RegressionModel cachedModel = modelCache.getCachedModel();
        if (cachedModel != null && cachedModel.isValid()) {
            log.debug("使用缓存的回归模型，版本: {}", cachedModel.getVersion());
            this.regressionModel = cachedModel;
            return;
        }

        // 训练新模型
        if (samples == null || samples.isEmpty()) {
            log.warn("训练样本为空，使用默认模型");
            this.regressionModel = createDefaultModel();
            return;
        }

        log.info("开始训练回归模型，样本数: {}", samples.size());
        this.regressionModel = modelTrainer.train(samples);

        // 缓存模型
        if (regressionModel.isValid()) {
            modelCache.cacheModel(regressionModel);
        }
    }

    /**
     * 强制重新训练模型（忽略缓存）
     */
    public void retrainModel() {
        modelCache.invalidate();
        trainModel();
    }

    /**
     * 创建默认模型
     */
    private RegressionModel createDefaultModel() {
        return RegressionModel.builder()
                .coefficients(new double[]{0.0, 1.0, 1.0, 1.0, 1.0, 1.0})
                .rSquared(0.0)
                .build();
    }

    /**
     * 获取当前回归模型
     * @return 回归模型
     */
    public RegressionModel getRegressionModel() {
        return regressionModel;
    }



    /**
     * 预测任务执行时间
     * @param cpuUtil CPU使用率
     * @param memUsage 内存使用率
     * @param powerRemain 剩余电量
     * @param storageRemain 剩余存储空间
     * @param latency 网络延迟
     * @return 预测的执行时间
     */
    private double predict(double cpuUtil, double memUsage, double powerRemain,
                           double storageRemain, double latency) {
        if (regressionModel == null) {
            throw new IllegalStateException("模型尚未训练");
        }
        return regressionModel.predict(cpuUtil, memUsage, powerRemain, storageRemain, latency);
    }

    /**
     * 使用特征数组预测
     * @param features 特征数组 [cpuUsage, memoryUsage, powerRemain, storageRemain, latency]
     * @return 预测的执行时间
     */
    private double predict(double[] features) {
        if (regressionModel == null) {
            throw new IllegalStateException("模型尚未训练");
        }
        return regressionModel.predict(features);
    }

    /**
     * 计算负载评分
     * 公式：S = w1*(1-CPU) + w2*(1-MEM) + w3*POWER + w4*STORAGE + w5*(1-LATENCY)
     * @param nodeFeatures 节点特征数组
     * @param omega 权重参数数组 [w1, w2, w3, w4, w5]
     * @return 负载评分（越高越好）
     */
    private double calculateLoadScore(double[] nodeFeatures, double[] omega) {
        return OptimizationSample.calculateLoadScore(nodeFeatures, omega);
    }

    /**
     * 目标1：任务延迟最小化
     * 基于权重参数选择最优节点，计算总预测执行时间
     * @param omega 权重参数数组 [w1, w2, w3, w4, w5]
     * @return 总预测执行时间
     */
    public double evaluateDurationWithOmega(double[] omega) {
        List<NodeSelectionResult> selections = selectNodesForAllTasks(omega);

        double totalDuration = 0.0;
        for (NodeSelectionResult selection : selections) {
            if (selection.getNodeFeatures() != null) {
                totalDuration += predict(selection.getNodeFeatures());
            }
        }

        return totalDuration;
    }

    /**
     * 目标2：终端设备负载均衡性
     * 计算终端间负载差异的方差均值
     * @param omega 权重参数数组 [w1, w2, w3, w4, w5]
     * @return 负载均衡指标（越小越好）
     */
    public double evaluateLoadBalancing(double[] omega) {
        List<NodeSelectionResult> selections = selectNodesForAllTasks(omega);

        // 收集每个终端的负载评分
        Map<String, Double> nodeLoadScores = new HashMap<>();
        Map<String, Integer> nodeTaskCounts = new HashMap<>();

        for (NodeSelectionResult selection : selections) {
            String nodeId = selection.getNodeIdentifier();
            nodeLoadScores.put(nodeId, nodeLoadScores.getOrDefault(nodeId, 0.0) + selection.getLoadScore());
            nodeTaskCounts.put(nodeId, nodeTaskCounts.getOrDefault(nodeId, 0) + 1);
        }

        // 计算每个终端的平均负载评分
        double totalAvgScore = 0.0;
        int nodeCount = 0;
        for (Map.Entry<String, Double> entry : nodeLoadScores.entrySet()) {
            double avgScore = entry.getValue() / nodeTaskCounts.get(entry.getKey());
            totalAvgScore += avgScore;
            nodeCount++;
        }

        if (nodeCount == 0) {
            return 0.0;
        }

        double globalAvgScore = totalAvgScore / nodeCount;

        // 计算方差均值
        double varianceSum = 0.0;
        for (Map.Entry<String, Double> entry : nodeLoadScores.entrySet()) {
            double avgScore = entry.getValue() / nodeTaskCounts.get(entry.getKey());
            varianceSum += Math.pow(avgScore - globalAvgScore, 2);
        }

        return varianceSum / nodeCount;
    }

    /**
     * 百分比转换因子：将0-100整数转换为0-1小数
     */
    public static final double PERCENTAGE_SCALE = 100.0;

    /**
     * 目标3：能耗控制
     * 基于剩余电量计算能耗指标
     * 适配整数百分比格式（0-100）
     * @param omega 权重参数数组 [w1, w2, w3, w4, w5]
     * @return 能耗指标（越小越好）
     */
    public double evaluateEnergyConsumption(double[] omega) {
        List<NodeSelectionResult> selections = selectNodesForAllTasks(omega);

        double totalEnergyCost = 0.0;
        for (NodeSelectionResult selection : selections) {
            if (selection.getNodeFeatures() != null) {
                // 能耗与剩余电量成反比，剩余电量越少，能耗成本越高
                // nodeFeatures[2] 是 powerRemain (0-100整数)
                double powerRemainNorm = selection.getNodeFeatures()[2] / PERCENTAGE_SCALE;
                totalEnergyCost += (1 - powerRemainNorm);
            }
        }

        return totalEnergyCost;
    }

    /**
     * 为所有任务选择节点（带缓存）
     * 这是性能优化的核心方法，避免重复计算
     * @param omega 权重参数
     * @return 节点选择结果列表
     */
    public List<NodeSelectionResult> selectNodesForAllTasks(double[] omega) {
        // 生成缓存键
        String cacheKey = generateOmegaKey(omega);

        // 检查缓存
        List<NodeSelectionResult> cached = selectionCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 执行节点选择
        List<NodeSelectionResult> results = new ArrayList<>();

        for (OptimizationSample sample : samples) {
            NodeSelectionResult selection = selectBestNode(sample, omega);
            if (selection != null) {
                results.add(selection);
            }
        }

        // 存入缓存
        selectionCache.put(cacheKey, results);

        return results;
    }

    /**
     * 为单个任务选择最佳节点
     * @param sample 任务样本
     * @param omega 权重参数
     * @return 节点选择结果
     */
    private NodeSelectionResult selectBestNode(OptimizationSample sample, double[] omega) {
        List<double[]> candidates = sample.getCandidateNodeFeatures();
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        int bestIndex = 0;
        double maxScore = Double.NEGATIVE_INFINITY;
        double[] bestFeatures = null;

        for (int i = 0; i < candidates.size(); i++) {
            double[] features = candidates.get(i);
            double score = calculateLoadScore(features, omega);

            if (score > maxScore) {
                maxScore = score;
                bestIndex = i;
                bestFeatures = features;
            }
        }

        return NodeSelectionResult.builder()
                .taskId(sample.getTaskId())
                .selectedNodeIndex(bestIndex)
                .loadScore(maxScore)
                .nodeFeatures(bestFeatures)
                .build();
    }

    /**
     * 生成omega参数的缓存键
     * @param omega 权重参数
     * @return 缓存键
     */
    private String generateOmegaKey(double[] omega) {
        // 使用4位小数精度生成键
        StringBuilder sb = new StringBuilder();
        for (double w : omega) {
            sb.append(String.format("%.4f_", w));
        }
        return sb.toString();
    }

    /**
     * 清除选择缓存
     */
    public void clearSelectionCache() {
        selectionCache.clear();
    }
}
