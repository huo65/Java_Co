package cn.huo.ohmqttserver.task;

import cn.huo.ohmqttserver.service.AliveService;
import org.dromara.mica.mqtt.core.server.MqttServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * @author wsq
 */
@Service
public class PublishAllTask {
	@Autowired
	private MqttServer mqttServer;
	@Autowired
	private AliveService aliveService;

	/**
	 * 测试连接
	 */
	@Scheduled(fixedDelay = 10000)
	public void run() {
		System.out.println("PublishAllTask run");
		mqttServer.publishAll("/test/123", "huo测试连接".getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * 存活节点广播
	 */
	@Scheduled(fixedDelay = 12000)
	public void alive() {
		System.out.println("PublishAllTask alive");
		Set<String> aliveList = aliveService.getAliveList();
		String message = null;
		if (aliveList!= null && !aliveList.isEmpty()) {
			message = "{\"aliveDevice\":[" + String.join(",", aliveList) + "]}";
		}
		if (message != null) {
			mqttServer.publishAll("/device/list", message.getBytes(StandardCharsets.UTF_8));
		}
	}

}
