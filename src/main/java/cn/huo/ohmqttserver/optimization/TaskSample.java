package cn.huo.ohmqttserver.optimization;

import jakarta.persistence.*;
import lombok.*;

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
public class TaskSample {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 候选节点列表（多个 NodeStatus）
	@OneToMany(mappedBy = "taskSample", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<NodeStatus> nodes = new ArrayList<>();

	// 实际被选中的节点（引用 NodeStatus）
	@OneToOne
	private NodeStatus choseNode;

	private double duration;

	public TaskSample(List<NodeStatus> nodes, NodeStatus choseNode, double duration) {
		this.nodes = nodes;
		this.choseNode = choseNode;
		this.duration = duration;

		for (NodeStatus ns : nodes) {
			ns.setTaskSample(this); // 建立反向引用
		}
	}

	// Getter/Setter 略
}
