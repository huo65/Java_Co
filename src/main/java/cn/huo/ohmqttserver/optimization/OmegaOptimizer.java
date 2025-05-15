package cn.huo.ohmqttserver.optimization;


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
 * @author huozj
 */
public class OmegaOptimizer {


	public static double[] optimize(List<TaskSample> samples) {
		Algorithm nsga2 = getAlgorithm(samples);

		NondominatedPopulation result = nsga2.getResult();

		Solution best = null;
		double minObj = Double.MAX_VALUE;
		for (Solution sol : result) {
			double obj = sol.getObjective(0);
			if (obj < minObj) {
				minObj = obj;
				best = sol;
			}
		}


		double[] omega = new double[4];
		for (int i = 0; i < 4; i++) {
			if (best != null) {
				omega[i] = EncodingUtils.getReal(best.getVariable(i));
			}
		}

		// 再次归一化
		double sum = Arrays.stream(omega).sum();
		for (int i = 0; i < 4; i++) {
			omega[i] /= sum;
		}

		return omega;
	}

	private static Algorithm getAlgorithm(List<TaskSample> samples) {
		TaskModelEvaluator evaluator = new TaskModelEvaluator(samples);
		Problem problem = new OmegaOptimizationProblem(evaluator);


		Variation variation = new CompoundVariation(
			new SBX(1.0, 15),
			new PM(1.0 / 4.0, 0.1)
		);

		Algorithm nsga2 = new NSGAII(
			problem,
			new NondominatedSortingPopulation(),
			null,
			new TournamentSelection(2),
			variation,
                () -> {
                    Solution[] pop = new Solution[100];
                    for (int i = 0; i < 100; i++) {
                        pop[i] = problem.newSolution();
                    }
                    return pop;
                }
        );

		int maxEvaluations = 5000;
		int evaluations = 0;
		while (evaluations < maxEvaluations) {
			nsga2.step();
			evaluations++;
		}
		return nsga2;
	}
}
