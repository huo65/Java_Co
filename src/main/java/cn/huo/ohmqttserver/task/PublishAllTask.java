package cn.huo.ohmqttserver.task;

import cn.huo.ohmqttserver.optimization.entity.NodeInfo;
import cn.huo.ohmqttserver.service.AliveService;
import cn.huo.ohmqttserver.service.OptimizationService;
import cn.huo.ohmqttserver.service.TaskSampleService;
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
	private OptimizationService optimizationService;
	@Autowired
	TaskSampleService taskSampleService;

	/**
	 * 测试连接
	 */
	@Scheduled(fixedDelay = 15000)
	public void timeTest() {
		System.out.println("PublishAllTask run");
		long currentTimeMillis = System.currentTimeMillis();
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("timeStamp", currentTimeMillis);
		String message = jsonObject.toString();
		mqttServer.publishAll("/test/123", message.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * 存活节点列表定时更新
	 */
	@Scheduled(fixedDelay = 12000)
	public void alive() {
		Set<String> aliveList = AliveService.getAliveList();
		String message = null;
		if (aliveList!= null && !aliveList.isEmpty()) {
			message = createAliveMessage(aliveList);
		}
		if (message != null) {
			System.out.println("PublishAllTask alive");
			mqttServer.publishAll("/device/list", message.getBytes(StandardCharsets.UTF_8));
		}
	}
	/**
	 * 参数定时更新
	 */
	@Scheduled(fixedDelay = 5000000)
	public void updateParameter() {
		System.out.println("PublishAllTask update");
		double[] param = optimizationService.updateParam();
		//训练数据不足
		if (param == null)
			return;
		String message = createParamMessage(param);
		mqttServer.publishAll("/optimization/param", message.getBytes(StandardCharsets.UTF_8));
	}

	public String createAliveMessage(Set<String> aliveList) {
		List<String> sortedList = new ArrayList<>(aliveList); // Set -> List
		JSONArray jsonArray = new JSONArray();
		for (String item : sortedList) {
			jsonArray.put(item);
		}

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("aliveDevice", jsonArray);

		return jsonObject.toString();
	}
	public String createParamMessage(double[] param) {
		JSONArray jsonArray = new JSONArray();
		for (double item : param) {
			jsonArray.put(item);
		}

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("param", jsonArray);

		return jsonObject.toString();
	}

	/**
	 * nodeInfoMap定时删除僵尸节点(1h)
	 */
	@Scheduled(fixedDelay = 3600000)
	public void clearNodeInfoMap() {
		Set<String> aliveList = AliveService.getAliveList();
		for (String deviceName : NodeInfo.getAllNodeInfos().keySet()) {
			if (!aliveList.contains(deviceName)) {
				NodeInfo.deleteNodeInfo(deviceName);
			}
		}
	}

}
