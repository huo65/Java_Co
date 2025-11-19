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
	public double memUse;
	public double powerRemain;
	public double storageRemain;

	@ManyToOne
	@JoinColumn(name = "task_sample_id")
	private TaskSample taskSample;


	public void setTaskSample(TaskSample taskSample) {
		this.taskSample = taskSample;
	}
	public NodeStatus(double cpuUtil, double memFree, double powerRemain, double storageRatio) {
		this.cpuUtil = cpuUtil;
		this.memUse = memFree;
		this.powerRemain = powerRemain;
		this.storageRemain = storageRatio;
	}

	// Getter/Setter 略
}
