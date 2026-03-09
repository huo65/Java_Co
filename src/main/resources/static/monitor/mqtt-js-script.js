class MqttJsMonitor {
    constructor() {
        this.apiUrl = '/proxy/mqtt';
        this.username = 'mica';
        this.password = 'mica';
        this.clients = [];
        this.filteredClients = [];
        this.previousClients = new Map();
        this.mqttClient = null;
        this.mqttMessageCount = 0;
        this.clientId = 'web-monitor-' + Date.now();
        this.currentSubscriptions = new Set();
        this.init();
    }

    init() {
        console.log('初始化MQTT监控页面...');
        this.setupUI();
        this.bindEvents();
        this.loadClients();
        this.startAutoRefresh(30000);
        
        //检查MQTT库状态
        if (typeof mqtt !== 'undefined') {
            this.logConnection('系统监控库加载成功', 'success');
        } else {
            this.logConnection('警告: 系统监控库未加载，连接功能可能不可用', 'warning');
        }
    }

    setupUI() {
        // 设置监控节点ID
        const clientIdSuffix = this.clientId.split('-')[2];
        document.getElementById('mqttClientId').value = this.clientId;
    }

    bindEvents() {
        document.getElementById('refreshBtn').addEventListener('click', () => {
            this.loadClients();
        });

        document.getElementById('connectBtn').addEventListener('click', () => {
            this.connectMqtt();
        });

        document.getElementById('disconnectBtn').addEventListener('click', () => {
            this.disconnectMqtt();
        });

        document.getElementById('subscribeBtn').addEventListener('click', () => {
            this.subscribeToTopic();
        });

        document.getElementById('unsubscribeBtn').addEventListener('click', () => {
            this.unsubscribeFromTopic();
        });

        document.getElementById('publishBtn').addEventListener('click', () => {
            this.publishMessage();
        });

        document.getElementById('searchInput').addEventListener('input', (e) => {
            this.filterClients();
        });

        document.getElementById('statusFilter').addEventListener('change', (e) => {
            this.filterClients();
        });

        // 添加配置输入框变化监听
        document.getElementById('mqttBrokerUrl').addEventListener('input', (e) => {
            // 服务器地址变化处理
        });

        // 添加回车键连接支持
        document.getElementById('mqttBrokerUrl').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.connectMqtt();
            }
        });

        document.getElementById('mqttClientId').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.connectMqtt();
            }
        });

        // 添加高级配置切换
        document.getElementById('toggleAdvancedConfig').addEventListener('click', (e) => {
            const advancedConfig = document.getElementById('advancedConfig');
            const button = e.target;
            
            if (advancedConfig.classList.contains('hidden')) {
                advancedConfig.classList.remove('hidden');
                button.textContent = '隐藏高级配置';
            } else {
                advancedConfig.classList.add('hidden');
                button.textContent = '显示高级配置';
            }
        });
    }

    connectMqtt() {
        try {
            //检查MQTT库是否已加载
            if (typeof mqtt === 'undefined') {
                this.logConnection('系统监控库未加载，请刷新页面重试', 'error');
                this.showNotification('系统监控库加载失败，请检查网络连接', 'error');
                return;
            }

            this.updateMqttStatus('connecting', '连接中...');
            
            // 获取连接配置
            const brokerUrl = document.getElementById('mqttBrokerUrl').value.trim();
            const clientId = document.getElementById('mqttClientId').value.trim() || ('web-monitor-' + Date.now());
            const username = document.getElementById('mqttUsername').value.trim();
            const password = document.getElementById('mqttPassword').value.trim();
            const keepalive = parseInt(document.getElementById('keepalive').value) || 60;
            const protocolId = document.getElementById('protocolId').value;
            const protocolVersion = parseInt(document.getElementById('protocolVersionSelect').value) || 4;
            const clean = document.getElementById('cleanSession').value === 'true';

            //验证必要参数
            if (!brokerUrl) {
                this.logConnection('请填写服务器地址', 'error');
                this.showNotification('请填写服务器地址', 'error');
                this.updateMqttStatus('disconnected', '未连接');
                return;
            }

            // MQTT连接选项
            const options = {
                keepalive: keepalive,
                clientId: clientId,
                protocolId: protocolId,
                protocolVersion: protocolVersion,
                clean: clean,
                reconnectPeriod: 3000,
                connectTimeout: 10 * 1000,
                username: username,
                password: password,
                will: {
                    topic: 'will/' + clientId,
                    payload: JSON.stringify({ status: 'offline', clientId: clientId }),
                    qos: 0,
                    retain: false
                }
            };

            this.logConnection(`正在连接到: ${brokerUrl} (监控ID: ${clientId})`);

            // 创建MQTT客户端
            this.mqttClient = mqtt.connect(brokerUrl, options);

            // 设置连接超时处理
            const connectTimeout = setTimeout(() => {
                if (this.mqttClient && !this.mqttClient.connected) {
                    this.logConnection('连接超时，请检查服务器地址和网络', 'error');
                    this.updateMqttStatus('disconnected', '连接超时');
                    this.updateButtonState(false);
                    this.showNotification('连接超时，请检查配置', 'error');
                }
            }, 15000);

            this.mqttClient.on('connect', () => {
                clearTimeout(connectTimeout);
                this.onMqttConnect();
            });

            this.mqttClient.on('message', (topic, message) => {
                this.onMqttMessage(topic, message);
            });

            this.mqttClient.on('error', (error) => {
                clearTimeout(connectTimeout);
                this.onMqttError(error);
            });

            this.mqttClient.on('close', () => {
                clearTimeout(connectTimeout);
                this.onMqttClose();
            });

            this.mqttClient.on('reconnect', () => {
                this.logConnection('正在重连...', 'warning');
                this.updateMqttStatus('connecting', '重连中...');
            });

            this.mqttClient.on('offline', () => {
                this.logConnection('客户端离线', 'warning');
                this.updateMqttStatus('disconnected', '已离线');
            });

        } catch (error) {
            this.logConnection(`监控连接异常: ${error.message}`, 'error');
            this.updateMqttStatus('disconnected', '连接异常');
            this.updateButtonState(false);
            this.showNotification(`连接异常: ${error.message}`, 'error');
        }
    }

    onMqttConnect() {
        this.logConnection('监控连接已建立', 'success');
        this.updateMqttStatus('connected', '已连接');
        this.updateButtonState(true);
        
        // 初始化时订阅默认主题
        this.subscribeToTopic('sys/clients/+/status');
        
        this.showNotification('监控连接成功', 'success');
    }

    onMqttMessage(topic, message) {
        this.mqttMessageCount++;
        document.getElementById('mqttMessageCount').textContent = this.mqttMessageCount;
        
        const msgStr = message.toString();
        this.logReceivedMessage(topic, msgStr);
        
        // 尝试解析消息并检测客户端状态变化
        try {
            const msgObj = JSON.parse(msgStr);
            this.processSystemMessage(topic, msgObj);
        } catch (e) {
            // 非JSON消息，尝试其他处理方式
            this.processNonJsonMessage(topic, msgStr);
        }
    }

    processSystemMessage(topic, msgObj) {
        // 检查是否是客户端状态变化消息
        if (topic.includes('sys/clients/') && (topic.includes('/status') || topic.includes('/connected'))) {
            const clientIdMatch = topic.match(/sys\/clients\/([^\/]+)\/status|connected/);
            const clientId = clientIdMatch ? clientIdMatch[1] : null;
            
            if (clientId) {
                if (msgObj.status === 'online' || topic.includes('connected')) {
                    this.showNotification(`设备 ${clientId} 已上线`, 'success');
                } else if (msgObj.status === 'offline' || topic.includes('disconnected')) {
                    this.showNotification(`设备 ${clientId} 已下线`, 'warning');
                }
            }
        }
    }

    processNonJsonMessage(topic, msgStr) {
        // 处理非JSON消息
        if (topic.includes('sys/clients/')) {
            const parts = topic.split('/');
            if (parts.length >= 3) {
                const clientId = parts[2]; // sys/clients/{clientId}/...
                if (msgStr.includes('connected') || msgStr.includes('online')) {
                    this.showNotification(`设备 ${clientId} 已连接`, 'success');
                } else if (msgStr.includes('disconnected') || msgStr.includes('offline')) {
                    this.showNotification(`设备 ${clientId} 已断开`, 'warning');
                }
            }
        }
    }

    onMqttError(error) {
        this.logConnection(`监控连接错误: ${error.message}`, 'error');
        this.updateMqttStatus('disconnected', '连接错误');
        this.updateButtonState(false);
    }

    onMqttClose() {
        this.logConnection('监控连接已关闭', 'warning');
        this.updateMqttStatus('disconnected', '已断开');
        this.updateButtonState(false);
    }

    disconnectMqtt() {
        if (this.mqttClient) {
            // 取消所有订阅
            this.currentSubscriptions.forEach(topic => {
                this.mqttClient.unsubscribe(topic);
            });
            
            this.mqttClient.end(true);
            this.mqttClient = null;
        }
    }

    subscribeToTopic(topic = null) {
        if (!this.mqttClient || this.mqttClient.disconnected) {
            this.logConnection('请先连接系统服务器', 'warning');
            return;
        }

        const topicToSubscribe = topic || document.getElementById('subscribeTopic').value;
        if (!topicToSubscribe) {
            this.logConnection('请输入订阅主题', 'warning');
            return;
        }

        this.mqttClient.subscribe(topicToSubscribe, (err) => {
            if (err) {
                this.logConnection(`订阅失败: ${err.message}`, 'error');
            } else {
                this.currentSubscriptions.add(topicToSubscribe);
                this.logConnection(`成功订阅主题: ${topicToSubscribe}`, 'success');
            }
        });
    }

    unsubscribeFromTopic() {
        if (!this.mqttClient || this.mqttClient.disconnected) {
            this.logConnection('请先连接系统服务器', 'warning');
            return;
        }

        const topic = document.getElementById('subscribeTopic').value;
        if (!topic) {
            this.logConnection('请输入取消订阅的主题', 'warning');
            return;
        }

        this.mqttClient.unsubscribe(topic, (err) => {
            if (err) {
                this.logConnection(`取消订阅失败: ${err.message}`, 'error');
            } else {
                this.currentSubscriptions.delete(topic);
                this.logConnection(`成功取消订阅: ${topic}`, 'success');
            }
        });
    }

    publishMessage() {
        if (!this.mqttClient || this.mqttClient.disconnected) {
            this.logConnection('请先连接系统服务器', 'warning');
            return;
        }

        const topic = document.getElementById('publishTopic').value;
        const payload = document.getElementById('publishPayload').value;

        if (!topic || !payload) {
            this.logConnection('请输入主题和消息内容', 'warning');
            return;
        }

        this.mqttClient.publish(topic, payload, (err) => {
            if (err) {
                this.logConnection(`发布消息失败: ${err.message}`, 'error');
            } else {
                this.logConnection(`消息已发布到: ${topic}`, 'success');
            }
        });
    }

    updateMqttStatus(status, text) {
        const statusElement = document.getElementById('mqttStatus');
        statusElement.className = `status-badge status-${status}`;
        statusElement.textContent = text;
    }

    updateButtonState(connected) {
        document.getElementById('connectBtn').disabled = connected;
        document.getElementById('disconnectBtn').disabled = !connected;
    }

    loadClients() {
        fetch(`${this.apiUrl}/clients?_page=1&_limit=10000`, {
            method: 'GET',
            headers: {
                'Authorization': 'Basic ' + btoa(this.username + ':' + this.password)
            }
        })
        .then(response => response.json())
        .then(data => {
            if (data.code === 1) {
                this.processClientsData(data);
            } else {
                throw new Error(`API错误: ${data.code}`);
            }
        })
        .catch(error => {
            this.logConnection(`获取设备失败: ${error.message}`, 'error');
        });
    }

    processClientsData(data) {
        let clients = [];
        if (data.data && data.data.list) {
            clients = data.data.list;
        } else {
            clients = data.data || [];
        }
        
        const updatedClients = clients.map(client => ({
            clientId: client.clientId,
            username: client.username || 'N/A',
            connected: client.connected,
            ipAddress: client.ipAddress || 'N/A',
            port: client.port || 'N/A',
            connectedAt: client.connectedAt || client.createdAt || Date.now(),
            protoName: client.protoName || 'N/A',
            protoVer: client.protoVer || 'N/A'
        }));
        
        this.detectStatusChanges(updatedClients);
        this.clients = updatedClients;
        this.filterClients();
        this.updateStats();
        this.renderClients();
        this.updateLastUpdateTime();
    }

    detectStatusChanges(currentClients) {
        const currentMap = new Map();
        currentClients.forEach(client => {
            currentMap.set(client.clientId, client);
        });
        
        currentClients.forEach(client => {
            const prevClient = this.previousClients.get(client.clientId);
            if (!prevClient) {
                this.showNotification(`设备 ${client.clientId} 已连接`, 'success');
            } else if (!prevClient.connected && client.connected) {
                this.showNotification(`设备 ${client.clientId} 已上线`, 'success');
            } else if (prevClient.connected && !client.connected) {
                this.showNotification(`设备 ${client.clientId} 已下线`, 'warning');
            }
        });
        
        this.previousClients.forEach((prevClient, clientId) => {
            if (!currentMap.has(clientId)) {
                this.showNotification(`设备 ${clientId}已断开连接`, 'warning');
            }
        });
        
        this.previousClients = currentMap;
    }

    filterClients() {
        const searchTerm = document.getElementById('searchInput').value.toLowerCase();
        const statusFilter = document.getElementById('statusFilter').value;

        this.filteredClients = this.clients.filter(client => {
            const matchesSearch = client.clientId.toLowerCase().includes(searchTerm) ||
                                (client.username && client.username.toLowerCase().includes(searchTerm));
            
            let matchesStatus = true;
            if (statusFilter) {
                matchesStatus = client.connected.toString() === statusFilter;
            }

            return matchesSearch && matchesStatus;
        });

        this.renderClients();
        this.updateStats();
    }

    renderClients() {
        const container = document.getElementById('clientList');
        if (this.filteredClients.length === 0) {
            container.innerHTML = '<div class="no-data">没有找到匹配的设备</div>';
            return;
        }

        container.innerHTML = this.filteredClients.map(client => 
            this.createClientCard(client)
        ).join('');
    }

    createClientCard(client) {
        const statusClass = client.connected ? 'status-online' : 'status-offline';
        const statusText = client.connected ? '在线' : '离线';
        const connectedAt = new Date(client.connectedAt).toLocaleString('zh-CN');
        
        return `
            <div class="device-card">
                <div class="device-header">
                    <div class="device-id">${this.escapeHtml(client.clientId)}</div>
                    <div class="device-status">
                        <span class="status-dot ${statusClass}"></span>
                        <span>${statusText}</span>
                    </div>
                </div>
                <div class="device-info">
                    <div class="info-row">
                        <span class="info-label">IP地址:</span>
                        <span class="info-value">${this.escapeHtml(client.ipAddress)}:${client.port}</span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">协议版本:</span>
                        <span class="info-value">${client.protoName} ${client.protoVer}</span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">连接时间:</span>
                        <span class="info-value">${connectedAt}</span>
                    </div>
                </div>
            </div>
        `;
    }

    updateStats() {
        // 确保统计逻辑正确计算设备状态
        const total = this.clients.length;
        const online = this.clients.filter(c => c.connected).length;
        const offline = total - online;

        document.getElementById('totalClients').textContent = total;
        document.getElementById('onlineClients').textContent = online;
        document.getElementById('offlineClients').textContent = offline;
    }

    logConnection(message, type = 'info') {
        const logContainer = document.getElementById('connectionLog');
        const entry = document.createElement('div');
        entry.className = `log-entry log-${type}`;
        entry.innerHTML = `
            <span class="log-time">[${new Date().toLocaleTimeString()}]</span>
            <span class="log-content">${message}</span>
        `;
        logContainer.appendChild(entry);
        logContainer.scrollTop = logContainer.scrollHeight;
        
        // 限制日志条数
        while (logContainer.children.length > 50) {
            logContainer.removeChild(logContainer.firstChild);
        }
    }

    logReceivedMessage(topic, payload) {
        const logContainer = document.getElementById('receivedMessages');
        
        // 移除"等待接收消息..."提示
        if (logContainer.querySelector('.no-data')) {
            logContainer.innerHTML = '';
        }
        
        const entry = document.createElement('div');
        entry.className = 'message-item';
        entry.innerHTML = `
            <div>
                <span class="message-topic">[${topic}]</span>
                <span class="message-timestamp">${new Date().toLocaleTimeString()}</span>
            </div>
            <div class="message-payload">${payload}</div>
        `;
        logContainer.appendChild(entry);
        logContainer.scrollTop = logContainer.scrollHeight;
        
        // 限制消息条数
        while (logContainer.children.length > 100) {
            logContainer.removeChild(logContainer.firstChild);
        }
    }

    updateLastUpdateTime() {
        const now = new Date();
        document.getElementById('lastUpdate').textContent = `上次更新: ${now.toLocaleTimeString('zh-CN')}`;
    }

    showNotification(message, type = 'info') {
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.textContent = message;
        document.body.appendChild(notification);
        setTimeout(() => notification.remove(), 3000);
    }

    startAutoRefresh(interval) {
        setInterval(() => {
            if (this.mqttClient && this.mqttClient.connected) {
                // MQTT连接正常时减少轮询频率
                this.loadClients();
            } else {
                // MQTT未连接时正常轮询
                this.loadClients();
            }
        }, interval);
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', () => {
    // 确保系统监控库已加载
    if (typeof mqtt === 'undefined') {
        console.error('系统监控库未加载，请检查网络连接');
        document.body.innerHTML = '<div style="padding: 20px; text-align: center;">系统监控库加载失败，请检查网络连接</div>';
    } else {
        new MqttJsMonitor();
    }
});