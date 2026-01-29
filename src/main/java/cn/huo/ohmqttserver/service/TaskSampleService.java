package cn.huo.ohmqttserver.service;

import cn.huo.ohmqttserver.optimization.dao.TaskSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 1. @ClassName TaskSampleService
 * 2. @Description 任务样本存储
 * 3. @Author huo
 * 4. @Date 2025/5/20 下午7:26
 */
@Service
public class TaskSampleService {
    @Autowired
    private TaskSampleRepository taskSampleRepository;

    List<TaskSample> getNewTaskSamples(){
        return taskSampleRepository.findAll();
    }

}
