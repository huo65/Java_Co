package cn.huo.ohmqttserver.optimization;

import lombok.Data;

/**
 * @author huo
 * @date 2025/05/15 21:13
 **/
@Data
public class NodeStatus {
	public double cpuUtil;
	public double memFree;
	public double powerRemain;
	public double storageRatio;

	public NodeStatus(double cpuUtil, double memFree, double powerRemain, double storageRatio) {
		this.cpuUtil = cpuUtil;
		this.memFree = memFree;
		this.powerRemain = powerRemain;
		this.storageRatio = storageRatio;
	}
	/**
	 * 节点的综合负载计算
	 * @param omega
	 * @return
	 */
	public double computeLoad(double[] omega) {
		return omega[0] * cpuUtil +
			omega[1] * memFree +
			omega[2] * powerRemain +
			omega[3] * storageRatio;
	}
}
