package cn.huo.ohmqttserver.service;

import cn.huo.ohmqttserver.optimization.dao.NodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NodeStatusRepository extends JpaRepository<NodeStatus, Long> {}