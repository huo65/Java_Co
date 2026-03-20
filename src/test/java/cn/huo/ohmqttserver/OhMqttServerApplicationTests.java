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
		// 使用整数百分比格式（0-100）
		NodeStatus node1 = new NodeStatus(70, 30, 80, 50, 20);
		NodeStatus node2 = new NodeStatus(40, 70, 40, 50, 40);
		NodeStatus node3 = new NodeStatus(60, 80, 60, 30, 30);

		List<NodeStatus> nodeList = Arrays.asList(node1, node2, node3);
		TaskSample task = new TaskSample("testTask1", nodeList, node1, 12.0);

		taskSampleRepository.save(task);
	}
	@Test
	void testOptimizeWithBalancedLoadSamples() {
		// 均衡负载场景测试数据：模拟当前未考虑负载均衡，选择的节点cpu占用很低，但内存占用很高（应该降低cpu的权重）
		// 使用整数百分比格式（0-100）
		List<OptimizationSample> balancedLoadSamples = Arrays.asList(
			createSample("balanced_load_cpu_heavy_1",
				Arrays.asList(
					new double[]{20, 80, 60, 50, 40}, // CPU低但内存高的节点
					new double[]{70, 30, 60, 50, 40}, // CPU高但内存低的节点
					new double[]{50, 50, 60, 50, 40}  // 均衡节点
				),
				0, 10.0 //节点cpu占用很低，但内存占用很高
			),
			createSample("balanced_load_cpu_heavy_2",
				Arrays.asList(
					new double[]{10, 90, 70, 40, 30},
					new double[]{80, 20, 50, 60, 50},
					new double[]{40, 60, 60, 50, 40}
				),
				0, 11.0
			),
			createSample("balanced_load_cpu_heavy_3",
				Arrays.asList(
					new double[]{30, 70, 50, 60, 50},
					new double[]{60, 40, 70, 40, 30},
					new double[]{50, 50, 60, 50, 40}
				),
				0, 9.5
			),
			createSample("balanced_load_cpu_heavy_4",
				Arrays.asList(
					new double[]{25, 75, 60, 50, 40},
					new double[]{75, 25, 60, 50, 40},
					new double[]{50, 50, 60, 50, 40}
				),
				0, 10.5
			),
			createSample("balanced_load_cpu_heavy_5",
				Arrays.asList(
					new double[]{15, 85, 70, 40, 30},
					new double[]{85, 15, 50, 60, 50},
					new double[]{50, 50, 60, 50, 40}
				),
				0, 11.5
			),
			createSample("balanced_load_cpu_heavy_6",
				Arrays.asList(
					new double[]{35, 65, 50, 60, 50},
					new double[]{65, 35, 70, 40, 30},
					new double[]{50, 50, 60, 50, 40}
				),
				0, 9.0
			),
			createSample("balanced_load_cpu_heavy_7",
				Arrays.asList(
					new double[]{20, 80, 80, 30, 20},
					new double[]{70, 30, 40, 70, 60},
					new double[]{50, 50, 60, 50, 40}
				),
				0, 12.0
			),
			createSample("balanced_load_cpu_heavy_8",
				Arrays.asList(
					new double[]{10, 90, 60, 50, 40},
					new double[]{90, 10, 60, 50, 40},
					new double[]{50, 50, 60, 50, 40}
				),
				0, 13.0
			),
			createSample("balanced_load_cpu_heavy_9",
				Arrays.asList(
					new double[]{30, 70, 70, 40, 30},
					new double[]{60, 40, 50, 60, 50},
					new double[]{45, 55, 60, 50, 40}
				),
				0, 8.5
			),
			createSample("balanced_load_cpu_heavy_10",
				Arrays.asList(
					new double[]{25, 75, 50, 60, 50},
					new double[]{75, 25, 70, 40, 30},
					new double[]{55, 45, 60, 50, 40}
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
	void testOptimizeWithPowerSensitiveSamples() {
		// 电量敏感场景测试数据：模拟当前的决策缺少对电量的考虑，选择的节点cpu占用很低，但电量也很低（应该提高电量的权重）
		// 使用整数百分比格式（0-100）
		List<OptimizationSample> powerSensitiveSamples = Arrays.asList(
			createSample("power_sensitive_low_power_1",
				Arrays.asList(
					new double[]{20, 30, 10, 80, 20}, // 负载低但电量很少的节点
					new double[]{60, 50, 80, 50, 40}, // 负载高但电量充足的节点
					new double[]{40, 40, 50, 60, 30}  // 均衡节点
				),
				0, 8.0//选择的节点cpu占用很低，但电量也很低
			),
			createSample("power_sensitive_low_power_2",
				Arrays.asList(
					new double[]{30, 40, 20, 70, 10},
					new double[]{50, 60, 70, 60, 50},
					new double[]{40, 50, 40, 50, 30}
				),
				0, 7.5
			),
			createSample("power_sensitive_low_power_3",
				Arrays.asList(
					new double[]{10, 20, 5, 90, 15},
					new double[]{70, 60, 90, 40, 60},
					new double[]{40, 40, 50, 60, 30}
				),
				0, 8.5
			),
			createSample("power_sensitive_low_power_4",
				Arrays.asList(
					new double[]{25, 35, 15, 85, 25},
					new double[]{65, 55, 85, 55, 45},
					new double[]{45, 45, 55, 65, 35}
				),
				0, 8.2
			),
			createSample("power_sensitive_low_power_5",
				Arrays.asList(
					new double[]{15, 25, 8, 88, 18},
					new double[]{55, 45, 75, 58, 48},
					new double[]{35, 35, 45, 68, 28}
				),
				0, 7.8
			),
			createSample("power_sensitive_low_power_6",
				Arrays.asList(
					new double[]{35, 45, 25, 75, 12},
					new double[]{60, 50, 80, 50, 40},
					new double[]{45, 45, 50, 60, 30}
				),
				0, 7.2
			),
			createSample("power_sensitive_low_power_7",
				Arrays.asList(
					new double[]{20, 30, 10, 90, 15},
					new double[]{70, 60, 85, 40, 55},
					new double[]{40, 40, 50, 60, 30}
				),
				0, 9.0
			),
			createSample("power_sensitive_low_power_8",
				Arrays.asList(
					new double[]{10, 10, 5, 95, 10},
					new double[]{80, 70, 90, 30, 60},
					new double[]{45, 45, 55, 65, 35}
				),
				0, 9.5
			),
			createSample("power_sensitive_low_power_9",
				Arrays.asList(
					new double[]{30, 40, 20, 80, 18},
					new double[]{50, 50, 70, 60, 45},
					new double[]{40, 40, 50, 60, 30}
				),
				0, 7.0
			),
			createSample("power_sensitive_low_power_10",
				Arrays.asList(
					new double[]{20, 20, 15, 85, 22},
					new double[]{60, 60, 80, 50, 48},
					new double[]{40, 40, 50, 60, 30}
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
