package cn.huo.ohmqttserver.optimization;

import cn.huo.ohmqttserver.optimization.dao.NodeStatus;
import cn.huo.ohmqttserver.optimization.dao.TaskSample;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.stat.regression.GLSMultipleLinearRegression;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务模型评估器，提供评估需要的方法
 * 基于文档要求实现动态优化LoadScore权重参数的评估功能
 * @author huozj
 */
public class TaskModelEvaluator {

    private final List<TaskSample> samples;
	private double[] beta = {0.0, 1.0, 1.0, 1.0, 1.0,1.0};

    /**
     * 构造函数
     * @param samples 任务样本列表，用于训练模型和评估
     */
    public TaskModelEvaluator(List<TaskSample> samples) {
        this.samples = samples;
    }

    /**
     * 训练线性回归模型，用于预测任务执行时间
     * 特征包括：CPU使用率、内存使用率、剩余电量、剩余存储空间、网络延迟
     */
    /**
     * 训练线性回归模型，用于预测任务执行时间
     * 特征包括：CPU使用率、内存使用率、剩余电量、剩余存储空间、网络延迟
     */
    public void trainModel(){
        int n = samples.size();
        double[] y = new double[n];
        double[][] x = new double[n][5];

        // 先填充数据
        for (int i = 0; i < n; i++) {
            TaskSample ts = samples.get(i);
            y[i] = ts.getDuration();
            // 转换特征为文档要求的格式
            x[i][0] = 1 - ts.getChoseNode().cpuUsage;
            x[i][1] = 1 - ts.getChoseNode().memoryUsage;
            x[i][2] = ts.getChoseNode().powerRemain;
            x[i][3] = ts.getChoseNode().storageRemain;
            x[i][4] = 1 - ts.getChoseNode().latency;
        }

        // 检查数据质量
        if (!validateData(x, y)) {
            System.out.println("数据验证失败，使用默认参数");
            this.beta = new double[]{0.0, 1.0, 1.0, 1.0, 1.0, 1.0}; // 默认系数
            return;
        }

        try {
            // 使用普通最小二乘法进行回归
            OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
            regression.newSampleData(y, x);
            this.beta = regression.estimateRegressionParameters();
        } catch (Exception e) {
            // 使用岭回归作为备选方案
            System.out.println("普通回归失败，使用岭回归方法: " + e.getMessage());
            this.beta = ridgeRegression(x, y, 0.001);
        }
    }

    /**
     * 数据验证方法
     */
    private boolean validateData(double[][] x, double[] y) {
        if (x.length < x[0].length) {
            System.out.println("样本数量(" + x.length + ")少于特征数量(" + x[0].length + ")");
            return false;
        }

        // 检查是否有重复行
        for (int i = 0; i < x.length; i++) {
            for (int j = i + 1; j < x.length; j++) {
                boolean isDuplicate = true;
                for (int k = 0; k < x[0].length; k++) {
                    if (Math.abs(x[i][k] - x[j][k]) > 1e-10) {
                        isDuplicate = false;
                        break;
                    }
                }
                if (isDuplicate) {
                    System.out.println("发现重复数据行: " + i + " 和 " + j);
                    return false;
                }
            }
        }

        return true;
    }


   /**
     * 岭回归实现
     */
    private double[] ridgeRegression(double[][] x, double[] y, double lambda) {
        RealMatrix X = new Array2DRowRealMatrix(x);
        RealVector Y = new ArrayRealVector(y);

        // 计算 X^T * X + λI
        RealMatrix XtX = X.transpose().multiply(X);
        RealMatrix identity = MatrixUtils.createRealIdentityMatrix(XtX.getColumnDimension());
        RealMatrix regularized = XtX.add(identity.scalarMultiply(lambda));

        // 分解求解
        DecompositionSolver solver = new LUDecomposition(regularized).getSolver();
        RealVector result = solver.solve(X.transpose().operate(Y));


        double[] coefficients = result.toArray();
        if (coefficients.length < 6) {
            double[] paddedCoefficients = new double[6];
            // 将原数组元素从索引1开始放置，索引0留作填充0
            System.arraycopy(coefficients, 0, paddedCoefficients, 6 - coefficients.length, coefficients.length);
            // 在首位填充0
            for (int i = 0; i < 6 - coefficients.length; i++) {
                paddedCoefficients[i] = 0;
            }
            return paddedCoefficients;
        }

        return coefficients;
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
	private double predict(double cpuUtil, double memUsage, double powerRemain, double storageRemain, double latency) {
		if (beta == null) {
			throw new IllegalStateException("模型尚未训练");
		}

		return beta[0] + beta[1]*(1 - cpuUtil) + beta[2]*(1 - memUsage) + 
		       beta[3]*powerRemain + beta[4]*storageRemain + beta[5]*(1 - latency);
	}

    /**
     * 计算负载评分
     * 公式：S = w1*(1-CPU) + w2*(1-MEM) + w3*POWER + w4*STORAGE + w5*(1-LATENCY)
     * @param node 节点状态
     * @param omega 权重参数数组 [w1, w2, w3, w4, w5]
     * @return 负载评分（越高越好）
     */
    private double calculateLoadScore(NodeStatus node, double[] omega) {
        return omega[0] * (1 - node.cpuUsage) + 
               omega[1] * (1 - node.memoryUsage) + 
               omega[2] * node.powerRemain + 
               omega[3] * node.storageRemain + 
               omega[4] * (1 - node.latency);
    }

    /**
     * 目标1：任务延迟最小化
     * 基于权重参数选择最优节点，计算总预测执行时间
     * @param omega 权重参数数组 [w1, w2, w3, w4, w5]
     * @return 总预测执行时间
     */
	public double evaluateDurationWithOmega(double[] omega) {
		double totalDuration = 0.0;

		for (TaskSample task : samples) {
			NodeStatus bestNode = null;
			double maxScore = Double.MIN_VALUE;

			for (NodeStatus node : task.getNodes()) {
				// 计算负载评分，选择评分最高的节点
				double score = calculateLoadScore(node, omega);

				if (score > maxScore) {
					maxScore = score;
					bestNode = node;
				}
			}

			// 用训练出的回归模型预测该任务在最优节点上的耗时
			double predictedDuration = 0;
			if (bestNode != null) {
				predictedDuration = predict(
					bestNode.cpuUsage,
					bestNode.memoryUsage,
					bestNode.powerRemain,
					bestNode.storageRemain,
					bestNode.latency
				);
			}

			totalDuration += predictedDuration;
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
        // 收集每个终端的负载评分
        Map<String, Double> nodeLoadScores = new HashMap<>();
        Map<String, Integer> nodeTaskCounts = new HashMap<>();

        for (TaskSample task : samples) {
            NodeStatus bestNode = null;
            double maxScore = Double.MIN_VALUE;

            for (NodeStatus node : task.getNodes()) {
                double score = calculateLoadScore(node, omega);
                if (score > maxScore) {
                    maxScore = score;
                    bestNode = node;
                }
            }

            if (bestNode != null) {
                String nodeId = bestNode.hashCode() + "";
                nodeLoadScores.put(nodeId, nodeLoadScores.getOrDefault(nodeId, 0.0) + maxScore);
                nodeTaskCounts.put(nodeId, nodeTaskCounts.getOrDefault(nodeId, 0) + 1);
            }
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
     * 目标3：能耗控制
     * 基于剩余电量计算能耗指标
     * @param omega 权重参数数组 [w1, w2, w3, w4, w5]
     * @return 能耗指标（越小越好）
     */
    public double evaluateEnergyConsumption(double[] omega) {
        double totalEnergyCost = 0.0;

        for (TaskSample task : samples) {
            NodeStatus bestNode = null;
            double maxScore = Double.MIN_VALUE;

            for (NodeStatus node : task.getNodes()) {
                double score = calculateLoadScore(node, omega);
                if (score > maxScore) {
                    maxScore = score;
                    bestNode = node;
                }
            }

            if (bestNode != null) {
                // 能耗与剩余电量成反比，剩余电量越少，能耗成本越高
                totalEnergyCost += (1 - bestNode.powerRemain);
            }
        }

        return totalEnergyCost;
    }
}
