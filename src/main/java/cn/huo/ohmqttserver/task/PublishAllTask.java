package cn.huo.ohmqttserver.task;

import cn.huo.ohmqttserver.service.AliveService;
import org.dromara.mica.mqtt.core.server.MqttServer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
	@Scheduled(fixedDelay = 15000)
	public void run() {
		System.out.println("PublishAllTask run");
		mqttServer.publishAll("/test/123", "huo测试连接".getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * 存活节点广播
	 */
	@Scheduled(fixedDelay = 12000)
	public void alive() {
		Set<String> aliveList = aliveService.getAliveList();
		String message = null;
		if (aliveList!= null && !aliveList.isEmpty()) {
			message = createMessage(aliveList);
		}
		if (message != null) {
			System.out.println("PublishAllTask alive");
			mqttServer.publishAll("/device/list", message.getBytes(StandardCharsets.UTF_8));
		}
	}
	public String createMessage(Set<String> aliveList) {
		List<String> sortedList = new ArrayList<>(aliveList); // Set -> List
		JSONArray jsonArray = new JSONArray();
		for (String item : sortedList) {
			jsonArray.put(item);
		}

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("aliveDevice", jsonArray);

		return jsonObject.toString();
	}

}
