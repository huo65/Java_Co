package cn.huo.ohmqttserver.optimization.dao;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author huozj
 * <a href="https://blog.csdn.net/qq_41320700/article/details/144031519">...</a>
 */
@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = {"nodes"})
@EntityListeners(AuditingEntityListener.class)
public class TaskSample {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String taskId;

	// 候选节点列表（多个 NodeStatus）
	@OneToMany(mappedBy = "taskSample", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<NodeStatus> nodes = new ArrayList<>();

	// 实际被选中的节点（引用 NodeStatus）
	@OneToOne
	private NodeStatus choseNode;

	private Double duration;

	/**
	 * 样本创建时间，用于获取最新样本
	 */
	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public TaskSample(String taskId,List<NodeStatus> nodes, NodeStatus choseNode, Double duration) {
		this.taskId = taskId;
		this.nodes = nodes;
		this.choseNode = choseNode;
		this.duration = duration;

		for (NodeStatus ns : nodes) {
			ns.setTaskSample(this); // 建立反向引用
		}
	}

	// Getter/Setter 略
}
