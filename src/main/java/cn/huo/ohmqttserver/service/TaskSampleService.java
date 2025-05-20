package cn.huo.ohmqttserver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 1. @ClassName TaskSampleService
 * 2. @Description TODO
 * 3. @Author huo
 * 4. @Date 2025/5/20 下午7:26
 */
@Service
public class TaskSampleService {
    @Autowired
    private TaskSampleRepository taskSampleRepository;

    void saveNewTask(){

    }

}
