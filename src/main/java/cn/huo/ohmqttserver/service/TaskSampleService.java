package cn.huo.ohmqttserver.service;

import cn.huo.ohmqttserver.optimization.dao.TaskSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 任务样本服务
 * 负责任务样本的查询和管理
 * @author huo
 */
@Service
public class TaskSampleService {

    private final TaskSampleRepository taskSampleRepository;

    /**
     * 最新样本数量限制，默认500条
     */
    @Value("${optimization.sample-limit:500}")
    private int sampleLimit;

    @Autowired
    public TaskSampleService(TaskSampleRepository taskSampleRepository) {
        this.taskSampleRepository = taskSampleRepository;
    }

    /**
     * 获取最新的任务样本（按创建时间排序，预加载关联数据）
     * 使用创建时间而非ID排序，确保获取真正最新的样本
     *
     * @return 任务样本列表（最多sampleLimit条）
     */
    @Transactional(readOnly = true)
    public List<TaskSample> getNewTaskSamples() {
        Pageable limit = PageRequest.of(0, sampleLimit);
        return taskSampleRepository.findLatestSamples(limit);
    }

    /**
     * 获取所有任务样本（预加载关联数据）
     * @return 任务样本列表
     */
    @Transactional(readOnly = true)
    public List<TaskSample> getAllTaskSamples() {
        return taskSampleRepository.findAllWithNodes();
    }

    /**
     * 清空任务样本
     */
    @Transactional
    public void clearTaskSample() {
        taskSampleRepository.deleteAll();
    }
}
