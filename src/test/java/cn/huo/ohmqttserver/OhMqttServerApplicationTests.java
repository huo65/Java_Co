package cn.huo.ohmqttserver;

import cn.huo.ohmqttserver.optimization.OmegaOptimizer;
import cn.huo.ohmqttserver.optimization.TaskSample;
import cn.huo.ohmqttserver.optimization.NodeStatus;
import cn.huo.ohmqttserver.service.TaskSampleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
class OhMqttServerApplicationTests {
	@Autowired
	private TaskSampleRepository taskSampleRepository;

	@Test
	public void saveSample() {
		NodeStatus node1 = new NodeStatus(0.7, 0.3, 0.8, 0.5);
		NodeStatus node2 = new NodeStatus(0.4, 0.7, 0.4, 0.5);
		NodeStatus node3 = new NodeStatus(0.6, 0.8, 0.6, 0.3);

		List<NodeStatus> nodeList = Arrays.asList(node1, node2, node3);
		TaskSample task = new TaskSample(nodeList, node1, 12.0);

		taskSampleRepository.save(task);
	}

	@Test
	void contextLoads() {
		List<TaskSample> samples = Arrays.asList(
			new TaskSample(
				Arrays.asList(
					new NodeStatus(0.7, 0.3, 0.8, 0.5),
					new NodeStatus(0.4, 0.7, 0.4, 0.5),
					new NodeStatus(0.6, 0.8, 0.6, 0.3)
				),
				new NodeStatus(0.7, 0.3, 0.8, 0.5), 12.0
			),
			new TaskSample(
				Arrays.asList(
					new NodeStatus(0.5, 0.5, 0.6, 0.4),
					new NodeStatus(0.3, 0.6, 0.5, 0.2),
					new NodeStatus(0.6, 0.4, 0.7, 0.3)
				),
				new NodeStatus(0.5, 0.5, 0.6, 0.4), 10.0
			),
			new TaskSample(
				Arrays.asList(
					new NodeStatus(0.8, 0.2, 0.9, 0.6),
					new NodeStatus(0.6, 0.5, 0.5, 0.4),
					new NodeStatus(0.7, 0.4, 0.8, 0.5)
				),
				new NodeStatus(0.8, 0.2, 0.9, 0.6), 14.0
			),
			new TaskSample(
				Arrays.asList(
					new NodeStatus(0.2, 0.9, 0.3, 0.1),
					new NodeStatus(0.3, 0.8, 0.5, 0.2),
					new NodeStatus(0.4, 0.6, 0.4, 0.3)
				),
				new NodeStatus(0.2, 0.9, 0.3, 0.1), 7.0
			),
			new TaskSample(
				Arrays.asList(
					new NodeStatus(0.6, 0.3, 0.8, 0.5),
					new NodeStatus(0.5, 0.6, 0.6, 0.4),
					new NodeStatus(0.4, 0.5, 0.7, 0.3)
				),
				new NodeStatus(0.6, 0.3, 0.8, 0.5), 11.5
			),
			new TaskSample(
				Arrays.asList(
					new NodeStatus(0.7, 0.2, 0.9, 0.7),
					new NodeStatus(0.8, 0.1, 0.9, 0.6),
					new NodeStatus(0.6, 0.3, 0.8, 0.5)
				),
				new NodeStatus(0.8, 0.1, 0.9, 0.6), 13.5
			),
			new TaskSample(
				Arrays.asList(
					new NodeStatus(0.3, 0.7, 0.6, 0.4),
					new NodeStatus(0.4, 0.6, 0.7, 0.3),
					new NodeStatus(0.2, 0.9, 0.4, 0.2)
				),
				new NodeStatus(0.2, 0.9, 0.4, 0.2), 8.0
			),
			new TaskSample(
				Arrays.asList(
					new NodeStatus(0.9, 0.1, 0.95, 0.7),
					new NodeStatus(0.7, 0.2, 0.8, 0.5),
					new NodeStatus(0.8, 0.3, 0.9, 0.6)
				),
				new NodeStatus(0.9, 0.1, 0.95, 0.7), 15.0
			),
			new TaskSample(
				Arrays.asList(
					new NodeStatus(0.5, 0.5, 0.5, 0.4),
					new NodeStatus(0.6, 0.4, 0.6, 0.5),
					new NodeStatus(0.4, 0.6, 0.4, 0.3)
				),
				new NodeStatus(0.5, 0.5, 0.5, 0.4), 9.0
			),
			new TaskSample(
				Arrays.asList(
					new NodeStatus(0.4, 0.6, 0.7, 0.4),
					new NodeStatus(0.5, 0.5, 0.6, 0.3),
					new NodeStatus(0.6, 0.4, 0.8, 0.5)
				),
				new NodeStatus(0.6, 0.4, 0.8, 0.5), 10.5
			)
		);


		double[] omega = OmegaOptimizer.optimize(samples);

		System.out.printf("Optimized ω values: [%.4f, %.4f, %.4f, %.4f]%n",
			omega[0], omega[1], omega[2], omega[3]);
	}

}
