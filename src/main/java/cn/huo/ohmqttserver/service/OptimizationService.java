package cn.huo.ohmqttserver.service;

import cn.huo.ohmqttserver.optimization.OmegaOptimizer;
import cn.huo.ohmqttserver.optimization.dao.TaskSample;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    TaskSampleService taskSampleService;

    public double[] updateParam(){
        List<TaskSample> samples = taskSampleService.getNewTaskSamples();
        if (samples.size() < 5){
            return null;
        }
        double[] omega = OmegaOptimizer.optimize(samples);
        System.out.printf("Optimized ω values (normal samples): [%.4f, %.4f, %.4f, %.4f, %.4f]%n",
                omega[0], omega[1], omega[2], omega[3], omega[4]);
        return omega;
    }

}
