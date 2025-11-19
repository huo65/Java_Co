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

	public double cpuUsage;
	public double memoryUsage;
	public double powerRemain;
	public double storageRemain;
	public double latency;

	@ManyToOne
	@JoinColumn(name = "task_sample_id")
	private TaskSample taskSample;


	public void setTaskSample(TaskSample taskSample) {
		this.taskSample = taskSample;
	}
	public NodeStatus(double cpuUsage, double memoryUsage, double powerRemain, double storageRemain, double latency) {
		this.cpuUsage = cpuUsage;
		this.memoryUsage = memoryUsage;
		this.powerRemain = powerRemain;
		this.storageRemain = storageRemain;
		this.latency = latency;
	}

	// Getter/Setter 略
}
