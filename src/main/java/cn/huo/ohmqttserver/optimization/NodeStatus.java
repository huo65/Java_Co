package cn.huo.ohmqttserver.optimization;

import jakarta.persistence.*;
import lombok.*;

/**
 * @author huo
 * @date 2025/05/15 21:13
 **/
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NodeStatus {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	public double cpuUtil;
	public double memFree;
	public double powerRemain;
	public double storageRatio;

	@ManyToOne
	@JoinColumn(name = "task_sample_id")
	private TaskSample taskSample;


	public void setTaskSample(TaskSample taskSample) {
		this.taskSample = taskSample;
	}
	public NodeStatus(double cpuUtil, double memFree, double powerRemain, double storageRatio) {
		this.cpuUtil = cpuUtil;
		this.memFree = memFree;
		this.powerRemain = powerRemain;
		this.storageRatio = storageRatio;
	}

	// Getter/Setter 略
}
