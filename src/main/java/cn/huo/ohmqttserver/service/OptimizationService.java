package cn.huo.ohmqttserver.service;

import cn.huo.ohmqttserver.optimization.OmegaOptimizer;
import cn.huo.ohmqttserver.optimization.dao.NodeStatus;
import cn.huo.ohmqttserver.optimization.dao.TaskSample;
import cn.huo.ohmqttserver.optimization.dto.OptimizationSample;
import cn.huo.ohmqttserver.optimization.validation.DataQualityChecker;
import cn.huo.ohmqttserver.optimization.validation.DataQualityReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 优化服务
 * 封装参数优化调用，支持数据验证和DTO转换
 * @author huo
 */
@Slf4j
@Service
public class OptimizationService {

    private final TaskSampleService taskSampleService;
    private final OmegaOptimizer omegaOptimizer;
    private final DataQualityChecker dataQualityChecker;

    @Autowired
    public OptimizationService(TaskSampleService taskSampleService,
                               OmegaOptimizer omegaOptimizer,
                               DataQualityChecker dataQualityChecker) {
        this.taskSampleService = taskSampleService;
        this.omegaOptimizer = omegaOptimizer;
        this.dataQualityChecker = dataQualityChecker;
    }

    /**
     * 更新优化参数
     * @return 优化后的权重参数数组，如果样本不足则返回null
     */
    public double[] updateParam() {
        List<TaskSample> entitySamples = taskSampleService.getNewTaskSamples();

        if (entitySamples.isEmpty()) {
            log.warn("没有可用的任务样本");
            return null;
        }

        // 转换为DTO
        List<OptimizationSample> samples = convertToDto(entitySamples);

        // 数据质量检查
        DataQualityReport report = dataQualityChecker.check(samples);
        log.info("数据质量检查: {}", report.getSummary());

        if (!report.isValid()) {
            log.error("数据质量检查未通过，无法进行优化");
            for (String error : report.getErrors()) {
                log.error("  - {}", error);
            }
            return null;
        }

        // 执行优化
        double[] omega = omegaOptimizer.optimize(samples);

        if (omega != null) {
            log.info("Optimized ω values: [{}, {}, {}, {}, {}]",
                    String.format("%.4f", omega[0]),
                    String.format("%.4f", omega[1]),
                    String.format("%.4f", omega[2]),
                    String.format("%.4f", omega[3]),
                    String.format("%.4f", omega[4]));
        }

        return omega;
    }

    /**
     * 将JPA实体转换为优化DTO
     * @param entitySamples JPA实体列表
     * @return DTO列表
     */
    private List<OptimizationSample> convertToDto(List<TaskSample> entitySamples) {
        List<OptimizationSample> dtoSamples = new ArrayList<>();

        for (TaskSample entity : entitySamples) {
            if (entity.getChoseNode() == null || entity.getNodes() == null) {
                continue;
            }

            // 提取候选节点特征
            List<double[]> candidateFeatures = new ArrayList<>();
            int chosenIndex = -1;

            for (int i = 0; i < entity.getNodes().size(); i++) {
                NodeStatus node = entity.getNodes().get(i);
                double[] features = extractFeatures(node);
                candidateFeatures.add(features);

                // 检查是否是选中的节点
                if (node.equals(entity.getChoseNode())) {
                    chosenIndex = i;
                }
            }

            // 如果没有找到选中的节点，使用第一个
            if (chosenIndex < 0 && !candidateFeatures.isEmpty()) {
                chosenIndex = 0;
            }

            if (!candidateFeatures.isEmpty() && chosenIndex >= 0) {
                OptimizationSample dto = OptimizationSample.builder()
                        .taskId(entity.getTaskId())
                        .candidateNodeFeatures(candidateFeatures)
                        .chosenNodeIndex(chosenIndex)
                        .duration(entity.getDuration() != null ? entity.getDuration() : 0.0)
                        .build();
                dtoSamples.add(dto);
            }
        }

        return dtoSamples;
    }

    /**
     * 从NodeStatus提取特征数组
     */
    private double[] extractFeatures(NodeStatus node) {
        return new double[]{
                node.getCpuUsage(),
                node.getMemoryUsage(),
                node.getPowerRemain(),
                node.getStorageRemain(),
                node.getLatency()
        };
    }

    /**
     * 获取优化配置信息
     */
    public String getOptimizationConfig() {
        return omegaOptimizer.getConfig().toString();
    }
}
