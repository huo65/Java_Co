package cn.huo.ohmqttserver.service;

import cn.huo.ohmqttserver.optimization.OmegaOptimizer;
import cn.huo.ohmqttserver.optimization.dao.TaskSample;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 1. @ClassName OptimizationService
 * 2. @Description 封装参数优化调用
 * 3. @Author huo
 * 4. @Date 2025/5/21 上午11:44
 */
@Service
public class OptimizationService {

//    TODO　存储完成后替换真实记录优化
    public double[] updateParam(){
        List<TaskSample> samples = Arrays.asList(
//                new TaskSample(
//                        Arrays.asList(
//                                new NodeStatus(0.7, 0.3, 0.8, 0.5),
//                                new NodeStatus(0.4, 0.7, 0.4, 0.5),
//                                new NodeStatus(0.6, 0.8, 0.6, 0.3)
//                        ),
//                        new NodeStatus(0.7, 0.3, 0.8, 0.5), 12.0
//                ),
//                new TaskSample(
//                        Arrays.asList(
//                                new NodeStatus(0.5, 0.5, 0.6, 0.4),
//                                new NodeStatus(0.3, 0.6, 0.5, 0.2),
//                                new NodeStatus(0.6, 0.4, 0.7, 0.3)
//                        ),
//                        new NodeStatus(0.5, 0.5, 0.6, 0.4), 10.0
//                ),
//                new TaskSample(
//                        Arrays.asList(
//                                new NodeStatus(0.8, 0.2, 0.9, 0.6),
//                                new NodeStatus(0.6, 0.5, 0.5, 0.4),
//                                new NodeStatus(0.7, 0.4, 0.8, 0.5)
//                        ),
//                        new NodeStatus(0.8, 0.2, 0.9, 0.6), 14.0
//                ),
//                new TaskSample(
//                        Arrays.asList(
//                                new NodeStatus(0.2, 0.9, 0.3, 0.1),
//                                new NodeStatus(0.3, 0.8, 0.5, 0.2),
//                                new NodeStatus(0.4, 0.6, 0.4, 0.3)
//                        ),
//                        new NodeStatus(0.2, 0.9, 0.3, 0.1), 7.0
//                ),
//                new TaskSample(
//                        Arrays.asList(
//                                new NodeStatus(0.6, 0.3, 0.8, 0.5),
//                                new NodeStatus(0.5, 0.6, 0.6, 0.4),
//                                new NodeStatus(0.4, 0.5, 0.7, 0.3)
//                        ),
//                        new NodeStatus(0.6, 0.3, 0.8, 0.5), 11.5
//                ),
//                new TaskSample(
//                        Arrays.asList(
//                                new NodeStatus(0.7, 0.2, 0.9, 0.7),
//                                new NodeStatus(0.8, 0.1, 0.9, 0.6),
//                                new NodeStatus(0.6, 0.3, 0.8, 0.5)
//                        ),
//                        new NodeStatus(0.8, 0.1, 0.9, 0.6), 13.5
//                ),
//                new TaskSample(
//                        Arrays.asList(
//                                new NodeStatus(0.3, 0.7, 0.6, 0.4),
//                                new NodeStatus(0.4, 0.6, 0.7, 0.3),
//                                new NodeStatus(0.2, 0.9, 0.4, 0.2)
//                        ),
//                        new NodeStatus(0.2, 0.9, 0.4, 0.2), 8.0
//                ),
//                new TaskSample(
//                        Arrays.asList(
//                                new NodeStatus(0.9, 0.1, 0.95, 0.7),
//                                new NodeStatus(0.7, 0.2, 0.8, 0.5),
//                                new NodeStatus(0.8, 0.3, 0.9, 0.6)
//                        ),
//                        new NodeStatus(0.9, 0.1, 0.95, 0.7), 15.0
//                ),
//                new TaskSample(
//                        Arrays.asList(
//                                new NodeStatus(0.5, 0.5, 0.5, 0.4),
//                                new NodeStatus(0.6, 0.4, 0.6, 0.5),
//                                new NodeStatus(0.4, 0.6, 0.4, 0.3)
//                        ),
//                        new NodeStatus(0.5, 0.5, 0.5, 0.4), 9.0
//                ),
//                new TaskSample(
//                        Arrays.asList(
//                                new NodeStatus(0.4, 0.6, 0.7, 0.4),
//                                new NodeStatus(0.5, 0.5, 0.6, 0.3),
//                                new NodeStatus(0.6, 0.4, 0.8, 0.5)
//                        ),
//                        new NodeStatus(0.6, 0.4, 0.8, 0.5), 10.5
//                )
        );
        double[] omega = OmegaOptimizer.optimize(samples);
        System.out.printf("Optimized ω values: [%.4f, %.4f, %.4f, %.4f]%n",
                omega[0], omega[1], omega[2], omega[3]);
        return omega;
    }
}
