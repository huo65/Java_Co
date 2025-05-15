package cn.huo.ohmqttserver;

import cn.huo.ohmqttserver.optimization.OmegaOptimizer;
import cn.huo.ohmqttserver.optimization.TaskSample;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
class OhMqttServerApplicationTests {

	@Test
	void contextLoads() {
		List<TaskSample> samples = Arrays.asList(
			new TaskSample(0.7, 0.3, 0.8, 0.5, 12),
			new TaskSample(0.6, 0.5, 0.6, 0.7, 10),
			new TaskSample(0.8, 0.2, 0.7, 0.4, 15),
			new TaskSample(0.4, 0.6, 0.5, 0.6, 9)
			// 从数据库加载更多调度数据
		);

		double[] omega = OmegaOptimizer.optimize(samples);

		System.out.printf("Optimized ω values: [%.4f, %.4f, %.4f, %.4f]%n",
			omega[0], omega[1], omega[2], omega[3]);
	}

}
