package cn.huo.ohmqttserver.service;

import cn.huo.ohmqttserver.optimization.NodeStatus;
import cn.huo.ohmqttserver.optimization.TaskSample;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskSampleRepository extends JpaRepository<TaskSample, Long> {}

