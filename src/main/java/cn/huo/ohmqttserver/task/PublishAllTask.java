package cn.huo.ohmqttserver.task;

import org.dromara.mica.mqtt.core.server.MqttServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * @author wsq
 */
@Service
public class PublishAllTask {
	@Autowired
	private MqttServer mqttServer;

	@Scheduled(fixedDelay = 100000)
	public void run() {
		System.out.println("PublishAllTask run");
		mqttServer.publishAll("/test/123", "mica最牛皮".getBytes(StandardCharsets.UTF_8));
	}

}
