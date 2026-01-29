package cn.huo.ohmqttserver.optimization;

import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.problem.AbstractProblem;

/**
 * 构造参数优化问题
 * 基于NSGA-II算法的多目标优化问题定义
 * @author huozj
 */
public class OmegaOptimizationProblem extends AbstractProblem {

    private final TaskModelEvaluator evaluator;

    /**
     * 构造函数
     * @param evaluator 任务模型评估器
     */
    public OmegaOptimizationProblem(TaskModelEvaluator evaluator) {
        super(5, 3); // 五个 omega 参数，三个优化目标
        this.evaluator = evaluator;
    }

    /**
     * 评估解决方案
     * @param solution 解决方案对象
     */
    @Override
    public void evaluate(Solution solution) {
        double[] omega = new double[5];
        double sum = 0.0;
        for (int i = 0; i < 5; i++) {
		omega[i] = EncodingUtils.getReal(solution.getVariable(i));
            sum += omega[i];
        }

        // 归一化：确保权重和为1
        if (sum > 0) {
            for (int i = 0; i < 5; i++) {
                omega[i] /= sum;
            }
        }

        // 目标1：任务延迟最小化
        double f1 = evaluator.evaluateDurationWithOmega(omega);
        
        // 目标2：终端设备负载均衡性
        double f2 = evaluator.evaluateLoadBalancing(omega);
        
        // 目标3：能耗控制
        double f3 = evaluator.evaluateEnergyConsumption(omega);

        solution.setObjective(0, f1);
        solution.setObjective(1, f2);
        solution.setObjective(2, f3);
    }

    /**
     * 创建新的解决方案
     * @return 新的解决方案对象
     */
    @Override
    public Solution newSolution() {
        Solution solution = new Solution(5, 3);
        for (int i = 0; i < 5; i++) {
		RealVariable realVariable = new RealVariable(0.0, 1.0);
		realVariable.randomize();
		solution.setVariable(i, realVariable); // ω 在 [0,1] 范围内
        }
        return solution;
    }
}
