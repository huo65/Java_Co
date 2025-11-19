package cn.huo.ohmqttserver.service;

import cn.huo.ohmqttserver.optimization.dao.TaskSample;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskSampleRepository extends JpaRepository<TaskSample, Long> {}

