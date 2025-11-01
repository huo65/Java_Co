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

	public double cpuIdle;
	public double memFree;
	public double powerRemain;
	public double storageIdle;

	@ManyToOne
	@JoinColumn(name = "task_sample_id")
	private TaskSample taskSample;


	public void setTaskSample(TaskSample taskSample) {
		this.taskSample = taskSample;
	}
	public NodeStatus(double cpuIdle, double memFree, double powerRemain, double storageIdle) {
		this.cpuIdle = cpuIdle;
		this.memFree = memFree;
		this.powerRemain = powerRemain;
		this.storageIdle = storageIdle;
	}

	// Getter/Setter 略
}
