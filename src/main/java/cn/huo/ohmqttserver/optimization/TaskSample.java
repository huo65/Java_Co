package cn.huo.ohmqttserver.optimization;

//import jakarta.persistence.Entity;
//import jakarta.persistence.Id;
import lombok.Data;

import java.util.List;

/**
 * @author huozj
 * <a href="https://blog.csdn.net/qq_41320700/article/details/144031519">...</a>
 */
@Data
//@Entity
public class TaskSample {
//	@Id
	private Long id;

	List<NodeStatus> nodes;

	NodeStatus choseNode;

	public double duration;

	public TaskSample(List<NodeStatus> nodes, NodeStatus choseNode, double duration) {
		this.nodes = nodes;
		this.choseNode = choseNode;
		this.duration = duration;
	}

	public TaskSample() {
	}


}
