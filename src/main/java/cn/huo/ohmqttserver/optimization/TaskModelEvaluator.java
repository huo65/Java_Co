package cn.huo.ohmqttserver.optimization;

import cn.huo.ohmqttserver.optimization.dao.NodeStatus;
import cn.huo.ohmqttserver.optimization.dao.TaskSample;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.util.List;

/**
 * @author huozj
 */
public class TaskModelEvaluator {

    private final List<TaskSample> samples;
	private double[] beta;

    public TaskModelEvaluator(List<TaskSample> samples) {
        this.samples = samples;
    }
	public void trainModel(){
//		以TaskSample中的数据，用线性回归模型来拟合duration和其余4个属性之间的关系
		int n = samples.size();
		double[] y = new double[n];         // duration
		double[][] x = new double[n][4];    // 特征向量：cpu, mem, power, storage



		for (int i = 0; i < n; i++) {
			TaskSample ts = samples.get(i);
			y[i] = ts.getDuration();
			x[i][0] = ts.getChoseNode().cpuUsage;
			x[i][1] = ts.getChoseNode().memoryUsage;
			x[i][2] = ts.getChoseNode().powerRemain;
			x[i][3] = ts.getChoseNode().storageRemain;
		}

		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
		regression.newSampleData(y, x);
		this.beta = regression.estimateRegressionParameters(); // β₀, β₁ ~ β₄
    }

	private double predict(double cpuUtil, double memFree, double powerRemain, double storageRatio) {
		if (beta == null) {
			throw new IllegalStateException("模型尚未训练");
		}

		return beta[0] + beta[1]*cpuUtil + beta[2]*memFree + beta[3]*powerRemain + beta[4]*storageRatio;
	}


//	    // 预测执行时间
//    public double evaluateDurationWithOmega(double[] omega) {
//        return samples.stream()
//            .mapToDouble(sample -> {
//                double load = sample.computeLoad(omega);
//                return Math.abs(sample.duration - load); // 简化目标函数
//            }).average().orElse(Double.MAX_VALUE);
//    }
	public double evaluateDurationWithOmega(double[] omega) {
		double totalDuration = 0.0;

		for (TaskSample task : samples) {
			NodeStatus bestNode = null;
			double minLoad = Double.MAX_VALUE;

			for (NodeStatus node : task.getNodes()) {
				// 计算负载评分 S_i^load
				double load = omega[0] * node.cpuUsage
					+ omega[1] * node.memoryUsage
					+ omega[2] * node.powerRemain
					+ omega[3] * node.storageRemain;

				if (load < minLoad) {
					minLoad = load;
					bestNode = node;
				}
			}

			// 用 训练出的回归模型 预测该任务在最优节点上的耗时
			double predictedDuration = 0;
			if (bestNode != null) {
				predictedDuration = predict(
					bestNode.cpuUsage,
					bestNode.memoryUsage,
					bestNode.powerRemain,
					bestNode.storageRemain
				);
			}

			totalDuration += predictedDuration;
		}

		return totalDuration;
	}



//	// 附加目标：资源使用波动越小越好（提高系统稳定性）
//    public double evaluateLoadVariance(double[] omega) {
//
//    }
}
