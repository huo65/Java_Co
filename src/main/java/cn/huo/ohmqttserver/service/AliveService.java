package cn.huo.ohmqttserver.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author huo
 * @date 2025/04/21 21:33
 **/
@Getter
@Service
public class AliveService {
	private Set<String> aliveList;
	public void initAliveList(){
		aliveList = new HashSet<>();
	}
	public void addNode(String deviceName){
		aliveList.add(deviceName);
	}

	public Set<String> getAliveList() {
		return aliveList;
	}
}
