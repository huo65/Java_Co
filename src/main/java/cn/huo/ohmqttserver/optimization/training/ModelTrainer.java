package cn.huo.ohmqttserver.optimization.training;

import cn.huo.ohmqttserver.optimization.dto.OptimizationSample;
import cn.huo.ohmqttserver.optimization.dto.RegressionModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 模型训练器
 * 负责训练线性回归模型并提供备选方案
 */
@Slf4j
@Component
public class ModelTrainer {

    /**
     * 默认回归系数（当训练失败时使用）
     */
    private static final double[] DEFAULT_COEFFICIENTS = {0.0, 1.0, 1.0, 1.0, 1.0, 1.0};

    /**
     * 岭回归正则化参数
     */
    private static final double RIDGE_LAMBDA = 0.001;

    /**
     * 训练模型
     *
     * @param samples 训练样本
     * @return 训练好的回归模型
     */
    public RegressionModel train(List<OptimizationSample> samples) {
        if (samples == null || samples.isEmpty()) {
            log.warn("训练样本为空，返回默认模型");
            return createDefaultModel();
        }

        int n = samples.size();
        double[] y = new double[n];
        double[][] x = new double[n][5];

        // 填充数据
        for (int i = 0; i < n; i++) {
            OptimizationSample sample = samples.get(i);
            y[i] = sample.getDuration();

            double[] chosenFeatures = sample.getChosenNodeFeatures();
            if (chosenFeatures != null && chosenFeatures.length >= 5) {
                // 转换特征: (1-CPU), (1-MEM), POWER, STORAGE, (1-LATENCY)
                x[i][0] = 1 - chosenFeatures[0];
                x[i][1] = 1 - chosenFeatures[1];
                x[i][2] = chosenFeatures[2];
                x[i][3] = chosenFeatures[3];
                x[i][4] = 1 - chosenFeatures[4];
            }
        }

        try {
            return performOLSRegression(x, y, samples.size());
        } catch (Exception e) {
            log.warn("普通最小二乘回归失败，尝试岭回归: {}", e.getMessage());
            try {
                return performRidgeRegression(x, y, samples.size());
            } catch (Exception e2) {
                log.error("岭回归也失败，使用默认模型: {}", e2.getMessage());
                return createDefaultModel();
            }
        }
    }

    /**
     * 执行普通最小二乘回归
     */
    private RegressionModel performOLSRegression(double[][] x, double[] y, int sampleCount) {
        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        regression.newSampleData(y, x);

        double[] coefficients = regression.estimateRegressionParameters();
        double rSquared = regression.calculateRSquared();
        double rmse = Math.sqrt(regression.calculateResidualSumOfSquares() / sampleCount);

        log.info("模型训练完成: R²={}, RMSE={}", String.format("%.4f", rSquared), String.format("%.4f", rmse));

        return RegressionModel.builder()
                .coefficients(coefficients)
                .trainedAt(LocalDateTime.now())
                .sampleCount(sampleCount)
                .rSquared(rSquared)
                .rmse(rmse)
                .version(UUID.randomUUID().toString().substring(0, 8))
                .build();
    }

    /**
     * 执行岭回归
     */
    private RegressionModel performRidgeRegression(double[][] x, double[] y, int sampleCount) {
        RealMatrix X = new Array2DRowRealMatrix(x);
        RealVector Y = new ArrayRealVector(y);

        // 计算 X^T * X + λI
        RealMatrix XtX = X.transpose().multiply(X);
        RealMatrix identity = MatrixUtils.createRealIdentityMatrix(XtX.getColumnDimension());
        RealMatrix regularized = XtX.add(identity.scalarMultiply(RIDGE_LAMBDA));

        // 分解求解
        DecompositionSolver solver = new LUDecomposition(regularized).getSolver();
        RealVector result = solver.solve(X.transpose().operate(Y));

        double[] coefficients = padCoefficients(result.toArray());

        // 计算近似的R²（岭回归没有直接的R²）
        double rSquared = 0.5; // 默认值

        log.info("岭回归模型训练完成");

        return RegressionModel.builder()
                .coefficients(coefficients)
                .trainedAt(LocalDateTime.now())
                .sampleCount(sampleCount)
                .rSquared(rSquared)
                .rmse(-1) // 未知
                .version(UUID.randomUUID().toString().substring(0, 8))
                .build();
    }

    /**
     * 填充系数数组到6个元素
     */
    private double[] padCoefficients(double[] coefficients) {
        if (coefficients.length >= 6) {
            return coefficients;
        }

        double[] padded = new double[6];
        System.arraycopy(coefficients, 0, padded, 6 - coefficients.length, coefficients.length);
        return padded;
    }

    /**
     * 创建默认模型
     */
    private RegressionModel createDefaultModel() {
        return RegressionModel.builder()
                .coefficients(DEFAULT_COEFFICIENTS.clone())
                .trainedAt(LocalDateTime.now())
                .sampleCount(0)
                .rSquared(0.0)
                .rmse(Double.MAX_VALUE)
                .version("default")
                .build();
    }
}
