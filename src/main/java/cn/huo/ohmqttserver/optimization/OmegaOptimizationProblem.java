package cn.huo.ohmqttserver.optimization;

import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.problem.AbstractProblem;

/**
 * @author huozj
 */
public class OmegaOptimizationProblem extends AbstractProblem {

    private final TaskModelEvaluator evaluator;

    public OmegaOptimizationProblem(TaskModelEvaluator evaluator) {
        super(4, 2); // 四个 omega，自定义两个优化目标
        this.evaluator = evaluator;
    }

    @Override
    public void evaluate(Solution solution) {
        double[] omega = new double[4];
        double sum = 0.0;
        for (int i = 0; i < 4; i++) {
			omega[i] = EncodingUtils.getReal(solution.getVariable(i));
            sum += omega[i];
        }

        // 归一化
        for (int i = 0; i < 4; i++) {
            omega[i] /= sum;
        }

        double f1 = evaluator.evaluateDurationWithOmega(omega);  // 目标1：任务耗时
        double f2 = evaluator.evaluateLoadVariance(omega);       // 目标2：资源负载波动

        solution.setObjective(0, f1);
        solution.setObjective(1, f2);
    }

    @Override
    public Solution newSolution() {
        Solution solution = new Solution(4, 2);
        for (int i = 0; i < 4; i++) {
            solution.setVariable(i, new RealVariable(0.0, 1.0));
        }
        return solution;
    }
}
