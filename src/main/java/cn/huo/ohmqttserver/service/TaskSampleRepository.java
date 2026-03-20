package cn.huo.ohmqttserver.service;

import cn.huo.ohmqttserver.optimization.dao.TaskSample;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskSampleRepository extends JpaRepository<TaskSample, Long> {

    /**
     * 查询所有任务样本，预加载nodes和choseNode关联
     * 按创建时间降序（最新的在前）
     */
    @EntityGraph(attributePaths = {"nodes", "choseNode"})
    @Query("SELECT t FROM TaskSample t ORDER BY t.createdAt DESC")
    List<TaskSample> findAllWithNodes();

    /**
     * 分页查询任务样本，预加载关联
     * 按创建时间降序（最新的在前）
     */
    @EntityGraph(attributePaths = {"nodes", "choseNode"})
    @Query("SELECT t FROM TaskSample t ORDER BY t.createdAt DESC")
    Page<TaskSample> findAllWithNodes(Pageable pageable);

    /**
     * 查询最新的N条任务样本
     * 按创建时间降序，预加载关联
     */
    @EntityGraph(attributePaths = {"nodes", "choseNode"})
    @Query("SELECT t FROM TaskSample t ORDER BY t.createdAt DESC")
    List<TaskSample> findLatestSamples(Pageable pageable);
}

