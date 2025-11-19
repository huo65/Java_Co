package cn.huo.ohmqttserver.listener;

import cn.huo.ohmqttserver.optimization.NodeInfo;
import cn.huo.ohmqttserver.optimization.NodeStatus;
import cn.huo.ohmqttserver.service.AliveService;
import lombok.val;
import org.dromara.mica.mqtt.codec.MqttPublishMessage;
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.core.server.event.IMqttMessageListener;
import org.dromara.mica.mqtt.spring.server.MqttServerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tio.core.ChannelContext;
import org.springframework.context.ApplicationContext;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static cn.huo.ohmqttserver.optimization.NodeInfo.*;

/**
 * 消息监听
 * @author huozj
 */
@Service
public class MqttServerMessageListener implements IMqttMessageListener, SmartInitializingSingleton {
	private static final Logger logger = LoggerFactory.getLogger(MqttServerMessageListener.class);
	@Autowired
	private ApplicationContext applicationContext;
	private MqttServerTemplate mqttServerTemplate;
    @Autowired
    private NodeInfo nodeInfo;
	@Autowired
	private AliveService aliveService;

	@Override
	public void onMessage(ChannelContext context, String clientId, String topic, MqttQoS qos, MqttPublishMessage message) {
		logger.info("context:{} topic:{} clientId:{} message:{} payload:{}", context,topic ,clientId, message, new String(message.payload(), StandardCharsets.UTF_8));
		if("/device/status".equals(topic)){
			String statusMessage = new String(message.payload());
			parseAndUpdateNodeInfo(statusMessage);
		}
		if ("task/assign".equals(topic)){
			String taskMessage = new String(message.payload());
			val allNodeInfos = getAllNodeInfos();
//			TODO 存储任务样本（除了完成时间）
			List<NodeStatus> nodeStatusList = new ArrayList<>();
			aliveService.getAliveList().forEach(deviceName -> {
				val nodeInfo = allNodeInfos.get(deviceName);
				if (nodeInfo != null) {
					nodeStatusList.add(new NodeStatus());
				}
			});

		}

	}

//	框架推荐做法，性能损失小
	@Override
	public void afterSingletonsInstantiated() {
		// 单利 bean 初始化完成之后从 ApplicationContext 中获取 bean
		mqttServerTemplate = applicationContext.getBean(MqttServerTemplate.class);
	}
}
