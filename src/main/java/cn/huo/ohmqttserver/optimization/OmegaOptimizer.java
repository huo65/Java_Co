package cn.huo.ohmqttserver.optimization;


import cn.huo.ohmqttserver.optimization.dao.TaskSample;
import org.moeaframework.algorithm.NSGAII;
import org.moeaframework.core.*;
import org.moeaframework.core.operator.CompoundVariation;
import org.moeaframework.core.operator.TournamentSelection;
import org.moeaframework.core.operator.real.PM;
import org.moeaframework.core.operator.real.SBX;
import org.moeaframework.core.variable.EncodingUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 参数优化器对外提供optimize方法
 * 基于NSGA-II算法的多目标优化实现
 * @author huozj
 */
public class OmegaOptimizer {

    /**
     * 优化权重参数
     * @param samples 任务样本列表
     * @return 优化后的权重参数数组 [w1, w2, w3, w4, w5]
     */
	public static double[] optimize(List<TaskSample> samples) {
		Algorithm nsga2 = getAlgorithm(samples);
		NondominatedPopulation result = nsga2.getResult();

		// 选择帕累托最优解中的最佳方案
		// 这里采用加权求和的方式选择最佳解
		Solution best = null;
		double bestScore = Double.MAX_VALUE;
		
		for (Solution sol : result) {
			// 计算加权目标值（所有目标都越小越好）
			// 权重可以根据实际需求调整
			double score = sol.getObjective(0) * 0.5 + // 任务延迟权重
			               sol.getObjective(1) * 0.3 + // 负载均衡权重
			               sol.getObjective(2) * 0.2; // 能耗控制权重
			
			if (score < bestScore) {
				bestScore = score;
				best = sol;
			}
		}

		// 提取权重参数
		double[] omega = new double[5];
		for (int i = 0; i < 5; i++) {
			if (best != null) {
				omega[i] = EncodingUtils.getReal(best.getVariable(i));
			}
		}

		// 再次归一化，确保权重和为1
		double sum = Arrays.stream(omega).sum();
		if (sum > 0) {
			for (int i = 0; i < 5; i++) {
				omega[i] /= sum;
			}
		}

		return omega;
	}

	/**
	 * 获取NSGA-II算法实例
	 * @param samples 任务样本列表
	 * @return NSGA-II算法实例
	 */
	private static Algorithm getAlgorithm(List<TaskSample> samples) {
		TaskModelEvaluator evaluator = new TaskModelEvaluator(samples);
		evaluator.trainModel();
		Problem problem = new OmegaOptimizationProblem(evaluator);

		// 变异操作：模拟二进制交叉 + 多项式变异
		Variation variation = new CompoundVariation(
			new SBX(1.0, 15),  // 交叉概率1.0，分布指数15
			new PM(1.0 / 5.0, 0.1)  // 变异概率1/5，分布指数0.1
		);

		// 初始化NSGA-II算法
		Algorithm nsga2 = new NSGAII(
			problem,
			new NondominatedSortingPopulation(),
			null,
			new TournamentSelection(2),  // 锦标赛选择，大小为2
			variation,
		        () -> {
		            // 初始化种群
		            Solution[] pop = new Solution[100];  // 种群大小100
		            for (int i = 0; i < 100; i++) {
		                pop[i] = problem.newSolution();
		            }
		            return pop;
		        }
	    );

		// 运行算法
		int maxEvaluations = 1000;  // 最大评估次数
		int evaluations = 0;
		while (evaluations < maxEvaluations) {
			nsga2.step();
			evaluations++;
		}
		return nsga2;
	}
}
