package cn.huo.ohmqttserver.optimization.validation;

import cn.huo.ohmqttserver.optimization.dto.OptimizationSample;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据质量检查器
 * 对训练数据进行全面的质量检查
 */
@Component
public class DataQualityChecker {

    /**
     * 最小样本量倍数（相对于特征数）
     */
    private static final int MIN_SAMPLE_MULTIPLIER = 10;

    /**
     * 异常值阈值（IQR倍数）
     */
    private static final double OUTLIER_THRESHOLD = 1.5;

    /**
     * 最大缺失值比例
     */
    private static final double MAX_MISSING_RATIO = 0.2;

    /**
     * 百分比数据的最大值（整数格式）
     */
    private static final double PERCENTAGE_MAX = 100.0;

    /**
     * 执行数据质量检查
     *
     * @param samples 样本列表
     * @return 数据质量报告
     */
    public DataQualityReport check(List<OptimizationSample> samples) {
        DataQualityReport report = DataQualityReport.builder()
                .valid(true)
                .totalSamples(samples.size())
                .build();

        if (samples.isEmpty()) {
            report.addError("样本列表为空");
            return report;
        }

        // 检查样本量
        checkSampleSize(samples, report);

        // 检查缺失值
        checkMissingValues(samples, report);

        // 检查异常值
        checkOutliers(samples, report);

        // 检查特征有效性
        checkFeatureValidity(samples, report);

        report.setValidSamples(calculateValidSamples(samples, report));

        return report;
    }

    /**
     * 检查样本量是否充足
     */
    private void checkSampleSize(List<OptimizationSample> samples, DataQualityReport report) {
        int n = samples.size();
        int p = 5; // 特征数量

        report.setFeatureCount(p);

        if (n < MIN_SAMPLE_MULTIPLIER * p) {
            report.addWarning(String.format(
                "样本数量(%d)可能不足，建议至少%d个样本",
                n, MIN_SAMPLE_MULTIPLIER * p
            ));
            report.setSampleSizeAdequate(false);
        } else {
            report.setSampleSizeAdequate(true);
        }
    }

    /**
     * 检查缺失值
     */
    private void checkMissingValues(List<OptimizationSample> samples, DataQualityReport report) {
        int missingCount = 0;
        int totalFields = 0;

        for (OptimizationSample sample : samples) {
            // 检查duration
            totalFields++;
            if (Double.isNaN(sample.getDuration()) || sample.getDuration() < 0) {
                missingCount++;
            }

            // 检查节点特征
            List<double[]> features = sample.getCandidateNodeFeatures();
            if (features == null || features.isEmpty()) {
                missingCount++;
            } else {
                for (double[] nodeFeatures : features) {
                    totalFields += nodeFeatures.length;
                    for (double value : nodeFeatures) {
                        if (Double.isNaN(value)) {
                            missingCount++;
                        }
                    }
                }
            }
        }

        double missingRatio = totalFields > 0 ? (double) missingCount / totalFields : 0;
        report.setMissingValueRatio(missingRatio);

        if (missingRatio > MAX_MISSING_RATIO) {
            report.addError(String.format("缺失值比例过高: %.2f%%", missingRatio * 100));
        } else if (missingRatio > 0) {
            report.addWarning(String.format("存在缺失值: %.2f%%", missingRatio * 100));
        }
    }

    /**
     * 检查异常值
     */
    private void checkOutliers(List<OptimizationSample> samples, DataQualityReport report) {
        // 收集duration值
        double[] durations = samples.stream()
                .mapToDouble(OptimizationSample::getDuration)
                .filter(d -> !Double.isNaN(d))
                .toArray();

        if (durations.length < 4) {
            return; // 样本太少，无法检测异常值
        }

        DescriptiveStatistics stats = new DescriptiveStatistics(durations);
        double q1 = stats.getPercentile(25);
        double q3 = stats.getPercentile(75);
        double iqr = q3 - q1;

        double lowerBound = q1 - OUTLIER_THRESHOLD * iqr;
        double upperBound = q3 + OUTLIER_THRESHOLD * iqr;

        int outlierCount = 0;
        for (double duration : durations) {
            if (duration < lowerBound || duration > upperBound) {
                outlierCount++;
            }
        }

        double outlierRatio = (double) outlierCount / durations.length;
        report.setOutlierRatio(outlierRatio);

        if (outlierRatio > 0.1) {
            report.addWarning(String.format("异常值比例较高: %.2f%%", outlierRatio * 100));
        }
    }

    /**
     * 检查特征有效性
     */
    private void checkFeatureValidity(List<OptimizationSample> samples, DataQualityReport report) {
        for (int i = 0; i < samples.size(); i++) {
            OptimizationSample sample = samples.get(i);

            // 检查候选节点
            if (sample.getCandidateNodeFeatures() == null ||
                sample.getCandidateNodeFeatures().isEmpty()) {
                report.addError(String.format("样本%d: 没有候选节点", i));
                continue;
            }

            // 检查选中节点索引
            if (sample.getChosenNodeIndex() < 0 ||
                sample.getChosenNodeIndex() >= sample.getCandidateNodeFeatures().size()) {
                report.addError(String.format("样本%d: 选中节点索引无效", i));
            }

            // 检查特征范围
            for (double[] features : sample.getCandidateNodeFeatures()) {
                if (features.length < 5) {
                    report.addError(String.format("样本%d: 特征维度不足", i));
                    break;
                }

                // 检查特征值是否在合理范围内 [0, 100]（整数百分比格式）
                for (int j = 0; j < 5; j++) {
                    if (features[j] < 0 || features[j] > PERCENTAGE_MAX) {
                        report.addWarning(String.format(
                            "样本%d: 特征%d超出范围[0,100]: %.2f", i, j, features[j]
                        ));
                    }
                }
            }
        }
    }

    /**
     * 计算有效样本数
     */
    private int calculateValidSamples(List<OptimizationSample> samples, DataQualityReport report) {
        return (int) samples.stream()
                .filter(s -> s.getDuration() >= 0 && !Double.isNaN(s.getDuration()))
                .filter(s -> s.getCandidateNodeFeatures() != null && !s.getCandidateNodeFeatures().isEmpty())
                .filter(s -> s.getChosenNodeIndex() >= 0 &&
                           s.getChosenNodeIndex() < s.getCandidateNodeFeatures().size())
                .count();
    }
}
