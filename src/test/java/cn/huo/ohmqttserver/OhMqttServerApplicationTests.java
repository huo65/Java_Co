package cn.huo.ohmqttserver;

import cn.huo.ohmqttserver.optimization.OmegaOptimizer;
import cn.huo.ohmqttserver.optimization.dao.TaskSample;
import cn.huo.ohmqttserver.optimization.dao.NodeStatus;
import cn.huo.ohmqttserver.optimization.dto.OptimizationSample;
import cn.huo.ohmqttserver.service.TaskSampleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OhMqttServerApplicationTests {

	@Autowired
	private TaskSampleRepository taskSampleRepository;

	@Autowired
	private OmegaOptimizer omegaOptimizer;

	@Test
	public void saveSample() {
		NodeStatus node1 = new NodeStatus(0.7, 0.3, 0.8, 0.5, 0.2);
		NodeStatus node2 = new NodeStatus(0.4, 0.7, 0.4, 0.5, 0.4);
		NodeStatus node3 = new NodeStatus(0.6, 0.8, 0.6, 0.3, 0.3);

		List<NodeStatus> nodeList = Arrays.asList(node1, node2, node3);
		TaskSample task = new TaskSample("testTask1", nodeList, node1, 12.0);

		taskSampleRepository.save(task);
	}
	@Test
	void testOptimizeWithBalancedLoadSamples() {
		// 均衡负载场景测试数据：CPU权重较大的参数配置环境
		// 测试数据为非理想选择：CPU低但内存高的节点，结果应该降低cpu的权重
		List<OptimizationSample> balancedLoadSamples = Arrays.asList(
			createSample("balanced_load_cpu_heavy_1",
				Arrays.asList(
					new double[]{0.2, 0.8, 0.6, 0.5, 0.4}, // CPU低但内存高的节点
					new double[]{0.7, 0.3, 0.6, 0.5, 0.4}, // CPU高但内存低的节点
					new double[]{0.5, 0.5, 0.6, 0.5, 0.4}  // 均衡节点
				),
				0, 10.0
			),
			createSample("balanced_load_cpu_heavy_2",
				Arrays.asList(
					new double[]{0.1, 0.9, 0.7, 0.4, 0.3},
					new double[]{0.8, 0.2, 0.5, 0.6, 0.5},
					new double[]{0.4, 0.6, 0.6, 0.5, 0.4}
				),
				0, 11.0
			),
			createSample("balanced_load_cpu_heavy_3",
				Arrays.asList(
					new double[]{0.3, 0.7, 0.5, 0.6, 0.5},
					new double[]{0.6, 0.4, 0.7, 0.4, 0.3},
					new double[]{0.5, 0.5, 0.6, 0.5, 0.4}
				),
				0, 9.5
			),
			createSample("balanced_load_cpu_heavy_4",
				Arrays.asList(
					new double[]{0.25, 0.75, 0.6, 0.5, 0.4},
					new double[]{0.75, 0.25, 0.6, 0.5, 0.4},
					new double[]{0.5, 0.5, 0.6, 0.5, 0.4}
				),
				0, 10.5
			),
			createSample("balanced_load_cpu_heavy_5",
				Arrays.asList(
					new double[]{0.15, 0.85, 0.7, 0.4, 0.3},
					new double[]{0.85, 0.15, 0.5, 0.6, 0.5},
					new double[]{0.5, 0.5, 0.6, 0.5, 0.4}
				),
				0, 11.5
			),
			createSample("balanced_load_cpu_heavy_6",
				Arrays.asList(
					new double[]{0.35, 0.65, 0.5, 0.6, 0.5},
					new double[]{0.65, 0.35, 0.7, 0.4, 0.3},
					new double[]{0.5, 0.5, 0.6, 0.5, 0.4}
				),
				0, 9.0
			),
			createSample("balanced_load_cpu_heavy_7",
				Arrays.asList(
					new double[]{0.2, 0.8, 0.8, 0.3, 0.2},
					new double[]{0.7, 0.3, 0.4, 0.7, 0.6},
					new double[]{0.5, 0.5, 0.6, 0.5, 0.4}
				),
				0, 12.0
			),
			createSample("balanced_load_cpu_heavy_8",
				Arrays.asList(
					new double[]{0.1, 0.9, 0.6, 0.5, 0.4},
					new double[]{0.9, 0.1, 0.6, 0.5, 0.4},
					new double[]{0.5, 0.5, 0.6, 0.5, 0.4}
				),
				0, 13.0
			),
			createSample("balanced_load_cpu_heavy_9",
				Arrays.asList(
					new double[]{0.3, 0.7, 0.7, 0.4, 0.3},
					new double[]{0.6, 0.4, 0.5, 0.6, 0.5},
					new double[]{0.45, 0.55, 0.6, 0.5, 0.4}
				),
				0, 8.5
			),
			createSample("balanced_load_cpu_heavy_10",
				Arrays.asList(
					new double[]{0.25, 0.75, 0.5, 0.6, 0.5},
					new double[]{0.75, 0.25, 0.7, 0.4, 0.3},
					new double[]{0.55, 0.45, 0.6, 0.5, 0.4}
				),
				0, 9.8
			)
		);

		// Execute optimization using Spring-managed optimizer
		double[] omega = omegaOptimizer.optimize(balancedLoadSamples);

		// Validate results
		assertNotNull(omega, "Omega array should not be null");
		assertEquals(5, omega.length, "Omega array should have length 5");

		// Check all elements are non-negative
		for (double w : omega) {
			assertTrue(w >= 0, "Omega values should be non-negative");
		}

		// Check sum is approximately 1.0 (due to normalization)
		double sum = Arrays.stream(omega).sum();
		assertEquals(1.0, sum, 0.01, "Omega values should sum to 1.0");

		// Print results for debugging
		System.out.printf("Optimized ω values (balanced load samples): [%.4f, %.4f, %.4f, %.4f, %.4f]%n",
				omega[0], omega[1], omega[2], omega[3], omega[4]);
	}

	/**
	 * 辅助方法：创建OptimizationSample
	 */
	private OptimizationSample createSample(String taskId, List<double[]> candidates, int chosenIndex, double duration) {
		return OptimizationSample.builder()
				.taskId(taskId)
				.candidateNodeFeatures(candidates)
				.chosenNodeIndex(chosenIndex)
				.duration(duration)
				.build();
	}

	@Test
	void testOptimizeWithNormalSamples() {
		// 电量敏感场景测试数据：对电量因素敏感的场景环境
		// 测试数据为非理想选择：CPU低且电量低的节点，结果应该提高电量的权重
		List<OptimizationSample> powerSensitiveSamples = Arrays.asList(
			createSample("power_sensitive_low_power_1",
				Arrays.asList(
					new double[]{0.2, 0.3, 0.1, 0.8, 0.2}, // 负载低但电量很少的节点
					new double[]{0.6, 0.5, 0.8, 0.5, 0.4}, // 负载高但电量充足的节点
					new double[]{0.4, 0.4, 0.5, 0.6, 0.3}  // 均衡节点
				),
				0, 8.0
			),
			createSample("power_sensitive_low_power_2",
				Arrays.asList(
					new double[]{0.3, 0.4, 0.2, 0.7, 0.1},
					new double[]{0.5, 0.6, 0.7, 0.6, 0.5},
					new double[]{0.4, 0.5, 0.4, 0.5, 0.3}
				),
				0, 7.5
			),
			createSample("power_sensitive_low_power_3",
				Arrays.asList(
					new double[]{0.1, 0.2, 0.05, 0.9, 0.15},
					new double[]{0.7, 0.6, 0.9, 0.4, 0.6},
					new double[]{0.4, 0.4, 0.5, 0.6, 0.3}
				),
				0, 8.5
			),
			createSample("power_sensitive_low_power_4",
				Arrays.asList(
					new double[]{0.25, 0.35, 0.15, 0.85, 0.25},
					new double[]{0.65, 0.55, 0.85, 0.55, 0.45},
					new double[]{0.45, 0.45, 0.55, 0.65, 0.35}
				),
				0, 8.2
			),
			createSample("power_sensitive_low_power_5",
				Arrays.asList(
					new double[]{0.15, 0.25, 0.08, 0.88, 0.18},
					new double[]{0.55, 0.45, 0.75, 0.58, 0.48},
					new double[]{0.35, 0.35, 0.45, 0.68, 0.28}
				),
				0, 7.8
			),
			createSample("power_sensitive_low_power_6",
				Arrays.asList(
					new double[]{0.35, 0.45, 0.25, 0.75, 0.12},
					new double[]{0.6, 0.5, 0.8, 0.5, 0.4},
					new double[]{0.45, 0.45, 0.5, 0.6, 0.3}
				),
				0, 7.2
			),
			createSample("power_sensitive_low_power_7",
				Arrays.asList(
					new double[]{0.2, 0.3, 0.1, 0.9, 0.15},
					new double[]{0.7, 0.6, 0.85, 0.4, 0.55},
					new double[]{0.4, 0.4, 0.5, 0.6, 0.3}
				),
				0, 9.0
			),
			createSample("power_sensitive_low_power_8",
				Arrays.asList(
					new double[]{0.1, 0.1, 0.05, 0.95, 0.1},
					new double[]{0.8, 0.7, 0.9, 0.3, 0.6},
					new double[]{0.45, 0.45, 0.55, 0.65, 0.35}
				),
				0, 9.5
			),
			createSample("power_sensitive_low_power_9",
				Arrays.asList(
					new double[]{0.3, 0.4, 0.2, 0.8, 0.18},
					new double[]{0.5, 0.5, 0.7, 0.6, 0.45},
					new double[]{0.4, 0.4, 0.5, 0.6, 0.3}
				),
				0, 7.0
			),
			createSample("power_sensitive_low_power_10",
				Arrays.asList(
					new double[]{0.2, 0.2, 0.15, 0.85, 0.22},
					new double[]{0.6, 0.6, 0.8, 0.5, 0.48},
					new double[]{0.4, 0.4, 0.5, 0.6, 0.3}
				),
				0, 8.8
			)
		);

		// Execute optimization using Spring-managed optimizer
		double[] omega = omegaOptimizer.optimize(powerSensitiveSamples);

		// Validate results
		assertNotNull(omega, "Omega array should not be null");
		assertEquals(5, omega.length, "Omega array should have length 5");

		// Check all elements are non-negative
		for (double w : omega) {
			assertTrue(w >= 0, "Omega values should be non-negative");
		}

		// Check sum is approximately 1.0 (due to normalization)
		double sum = Arrays.stream(omega).sum();
		assertEquals(1.0, sum, 0.01, "Omega values should sum to 1.0");

		// Print results for debugging
		System.out.printf("Optimized ω values (power sensitive samples): [%.4f, %.4f, %.4f, %.4f, %.4f]%n",
			omega[0], omega[1], omega[2], omega[3], omega[4]);
	}


}
