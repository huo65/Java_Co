package cn.huo.ohmqttserver.service;

import cn.huo.ohmqttserver.optimization.dao.TaskSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        Pageable limit = PageRequest.of(0, 500);
        return taskSampleRepository.findAll(limit).getContent();
    }
    void clearTaskSample(){
        taskSampleRepository.deleteAll();
    }


}
