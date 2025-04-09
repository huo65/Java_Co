package cn.huo.ohmqttserver.controller;


import cn.huo.ohmqttserver.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping("/mqtt/server")
@RestController
public class ServerController {
	@Autowired
	private ServerService service;


	@PostMapping("publish")
	public boolean publish(@RequestBody String body) {
		return service.publish(body);
	}

}
