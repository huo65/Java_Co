package cn.huo.ohmqttserver.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@RestController
@RequestMapping("/proxy/mqtt")
public class MqttProxyController {
    
    private final RestTemplate restTemplate;
    
    public MqttProxyController() {
        this.restTemplate = new RestTemplate();
    }
    
    @GetMapping("/clients")
    public ResponseEntity<String> getClients(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10000") int limit) {
        
        String url = String.format("http://localhost:8083/api/v1/clients?_page=%d&_limit=%d", page, limit);
        return proxyRequest(url);
    }
    
    @GetMapping("/client/subscriptions")
    public ResponseEntity<String> getClientSubscriptions(@RequestParam String clientId) {
        String url = String.format("http://localhost:8083/api/v1/client/subscriptions?clientId=%s", clientId);
        return proxyRequest(url);
    }
    
    private ResponseEntity<String> proxyRequest(String url) {
        try {
            // 设置认证头
            String auth = "mica:mica";
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encodedAuth);
            
            org.springframework.http.HttpEntity<String> entity = 
                new org.springframework.http.HttpEntity<>(headers);
            
            return restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}