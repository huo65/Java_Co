package cn.huo.ohmqttserver.optimization.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据质量报告
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityReport {

    /**
     * 是否通过验证
     */
    private boolean valid;

    /**
     * 样本总数
     */
    private int totalSamples;

    /**
     * 有效样本数
     */
    private int validSamples;

    /**
     * 缺失值比例
     */
    private double missingValueRatio;

    /**
     * 异常值比例
     */
    private double outlierRatio;

    /**
     * 特征数量
     */
    private int featureCount;

    /**
     * 样本量是否充足
     */
    private boolean sampleSizeAdequate;

    /**
     * 警告信息列表
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * 错误信息列表
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * 添加警告
     */
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }

    /**
     * 添加错误
     */
    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
        valid = false;
    }

    /**
     * 获取摘要信息
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("数据质量报告: ");
        sb.append(valid ? "通过" : "未通过");
        sb.append(String.format(" | 样本: %d/%d", validSamples, totalSamples));
        sb.append(String.format(" | 缺失值: %.2f%%", missingValueRatio * 100));
        sb.append(String.format(" | 异常值: %.2f%%", outlierRatio * 100));
        return sb.toString();
    }
}
