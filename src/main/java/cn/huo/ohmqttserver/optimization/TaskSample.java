package cn.huo.ohmqttserver.optimization;

//import jakarta.persistence.Entity;
//import jakarta.persistence.Id;
import lombok.Data;

/**
 * @author huozj
 * <a href="https://blog.csdn.net/qq_41320700/article/details/144031519">...</a>
 */
@Data
//@Entity
public class TaskSample {
//	@Id
	private Long id;
	public double cpuUtil;
	public double memFree;
	public double powerRemain;
	public double storageRatio;
	public double duration;

	public TaskSample(double cpuUtil, double memFree, double powerRemain, double storageRatio, double duration) {
		this.cpuUtil = cpuUtil;
		this.memFree = memFree;
		this.powerRemain = powerRemain;
		this.storageRatio = storageRatio;
		this.duration = duration;
	}

	public TaskSample() {

	}

	public double computeLoad(double[] omega) {
		return omega[0] * cpuUtil +
			omega[1] * memFree +
			omega[2] * powerRemain +
			omega[3] * storageRatio;
	}
}
