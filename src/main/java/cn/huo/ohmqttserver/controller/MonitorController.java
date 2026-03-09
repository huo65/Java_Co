package cn.huo.ohmqttserver.controller;


import cn.huo.ohmqttserver.optimization.entity.NodeInfo;
import cn.huo.ohmqttserver.service.AliveService;
import cn.huo.ohmqttserver.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;


/**
 * @author huozj
 */
@RequestMapping("/monitor")
@RestController
public class MonitorController {


	@GetMapping("/nodeInfo")
	public Map<String, NodeInfo> getAllNodeInfos() {
		return NodeInfo.getAllNodeInfos();
	}
	@GetMapping("/alive")
	public Set<String> getAlive() {
		return AliveService.getAliveList();
	}

}
