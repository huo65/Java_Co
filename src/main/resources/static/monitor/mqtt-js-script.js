/**
 * MQTT 监控系统 - JavaScript 模块
 * 版本: 2.0
 * 特点: 模块化设计、错误处理、性能优化
 */

class MqttJsMonitor {
    constructor() {
        // 配置常量
        this.config = {
            apiUrl: '/proxy/mqtt',
            monitorApiUrl: '/monitor',
            username: 'mica',
            password: 'mica',
            autoRefreshInterval: 30000,
            maxLogEntries: 50,
            maxMessageEntries: 100,
            connectTimeout: 15000,
            defaultBrokerUrl: 'ws://localhost:8083/mqtt'
        };
        
        // 状态管理
        this.state = {
            clients: [],
            filteredClients: [],
            previousClients: new Map(),
            nodeInfos: {},
            aliveDevices: new Set(),
            mqttClient: null,
            mqttMessageCount: 0,
            connectionLogCount: 0,
            isConnecting: false,
            currentSubscriptions: new Set()
        };
        
        // 生成客户端ID
        this.clientId = `monitor-${Date.now()}`;
        
        // DOM 元素缓存
        this.elements = {};
        
        // 初始化
        this.init();
    }

    /**
     * 初始化系统
     */
    init() {
        console.log('[MQTT Monitor] 初始化监控页面...');
        
        this.cacheElements();
        this.setupUI();
        this.bindEvents();
        this.loadClients();
        this.startAutoRefresh();
        
        // 检查MQTT库状态
        if (typeof mqtt !== 'undefined') {
            this.logConnection('系统监控库加载成功', 'success');
        } else {
            this.logConnection('警告: 系统监控库未加载，连接功能可能不可用', 'warning');
            this.showNotification('MQTT库加载失败，请检查网络连接', 'warning');
        }
    }

    /**
     * 缓存 DOM 元素引用
     */
    cacheElements() {
        const ids = [
            'mqttClientId', 'mqttBrokerUrl', 'mqttUsername', 'mqttPassword',
            'keepalive', 'protocolId', 'protocolVersionSelect', 'cleanSession',
            'mqttStatus', 'connectBtn', 'disconnectBtn',
            'subscribeBtn', 'unsubscribeBtn', 'subscribeTopic',
            'publishBtn', 'publishTopic', 'publishPayload',
            'toggleAdvancedConfig', 'advancedConfig',
            'receivedMessages', 'connectionLog', 'mqttMessageCount',
            'searchInput', 'statusFilter', 'clientList',
            'totalClients', 'onlineClients', 'offlineClients',
            'refreshBtn', 'lastUpdate', 'deviceMessageCount'
        ];
        
        ids.forEach(id => {
            this.elements[id] = document.getElementById(id);
        });
    }

    /**
     * 设置 UI 初始状态
     */
    setupUI() {
        if (this.elements.mqttClientId) {
            this.elements.mqttClientId.value = this.clientId;
        }
        
        // 设置初始连接日志
        this.logConnection('监控系统已就绪', 'info');
    }

    /**
     * 绑定事件处理器
     */
    bindEvents() {
        // 刷新按钮
        this.elements.refreshBtn?.addEventListener('click', () => {
            this.loadClients();
        });

        // MQTT 连接控制
        this.elements.connectBtn?.addEventListener('click', () => {
            this.connectMqtt();
        });

        this.elements.disconnectBtn?.addEventListener('click', () => {
            this.disconnectMqtt();
        });

        // 订阅控制
        this.elements.subscribeBtn?.addEventListener('click', () => {
            this.subscribeToTopic();
        });

        this.elements.unsubscribeBtn?.addEventListener('click', () => {
            this.unsubscribeFromTopic();
        });

        // 消息发布
        this.elements.publishBtn?.addEventListener('click', () => {
            this.publishMessage();
        });

        // 筛选器
        this.elements.searchInput?.addEventListener('input', this.debounce(() => {
            this.filterClients();
        }, 300));

        this.elements.statusFilter?.addEventListener('change', () => {
            this.filterClients();
        });

        // 高级配置切换
        this.elements.toggleAdvancedConfig?.addEventListener('click', (e) => {
            this.toggleAdvancedConfig(e.currentTarget);
        });

        // 回车键快捷操作
        this.bindEnterKeyHandlers();
    }

    /**
     * 绑定回车键快捷操作
     */
    bindEnterKeyHandlers() {
        const inputs = [
            { id: 'mqttBrokerUrl', action: () => this.connectMqtt() },
            { id: 'mqttClientId', action: () => this.connectMqtt() },
            { id: 'subscribeTopic', action: () => this.subscribeToTopic() },
            { id: 'publishTopic', action: () => this.publishMessage() },
            { id: 'publishPayload', action: () => this.publishMessage() }
        ];

        inputs.forEach(({ id, action }) => {
            this.elements[id]?.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    action();
                }
            });
        });
    }

    /**
     * 防抖函数
     */
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    /**
     * 切换高级配置显示
     */
    toggleAdvancedConfig(button) {
        const config = this.elements.advancedConfig;
        if (!config || !button) return;
        
        const isHidden = config.classList.contains('hidden');
        
        if (isHidden) {
            config.classList.remove('hidden');
            config.setAttribute('aria-hidden', 'false');
            button.setAttribute('aria-expanded', 'true');
        } else {
            config.classList.add('hidden');
            config.setAttribute('aria-hidden', 'true');
            button.setAttribute('aria-expanded', 'false');
        }
    }

    // ==================== MQTT 连接管理 ====================

    /**
     * 建立 MQTT 连接
     */
    connectMqtt() {
        if (this.state.isConnecting) {
            this.logConnection('正在连接中，请稍候...', 'warning');
            return;
        }

        // 检查MQTT库
        if (typeof mqtt === 'undefined') {
            this.logConnection('系统监控库未加载，请刷新页面重试', 'error');
            this.showNotification('系统监控库加载失败，请检查网络连接', 'error');
            return;
        }

        try {
            this.state.isConnecting = true;
            this.updateMqttStatus('connecting', '连接中...');
            
            // 获取连接配置
            const options = this.buildConnectionOptions();
            
            // 验证配置
            if (!options.brokerUrl) {
                throw new Error('请填写服务器地址');
            }

            this.logConnection(`正在连接到: ${options.brokerUrl}`);

            // 创建MQTT客户端
            this.state.mqttClient = mqtt.connect(options.brokerUrl, options.mqttOptions);

            // 设置连接超时
            const connectTimeout = setTimeout(() => {
                if (this.state.mqttClient && !this.state.mqttClient.connected) {
                    this.handleConnectionTimeout();
                }
            }, this.config.connectTimeout);

            // 绑定事件处理器
            this.bindMqttEvents(connectTimeout);

        } catch (error) {
            this.handleConnectionError(error);
        }
    }

    /**
     * 构建连接配置
     */
    buildConnectionOptions() {
        const brokerUrl = this.elements.mqttBrokerUrl?.value.trim();
        const clientId = this.elements.mqttClientId?.value.trim() || this.clientId;
        const username = this.elements.mqttUsername?.value.trim();
        const password = this.elements.mqttPassword?.value;
        const keepalive = parseInt(this.elements.keepalive?.value) || 60;
        const protocolId = this.elements.protocolId?.value || 'MQTT';
        const protocolVersion = parseInt(this.elements.protocolVersionSelect?.value) || 4;
        const clean = this.elements.cleanSession?.value === 'true';

        const mqttOptions = {
            keepalive,
            clientId,
            protocolId,
            protocolVersion,
            clean,
            reconnectPeriod: 3000,
            connectTimeout: 10000,
            username,
            password,
            will: {
                topic: `will/${clientId}`,
                payload: JSON.stringify({ status: 'offline', clientId }),
                qos: 0,
                retain: false
            }
        };

        return { brokerUrl, mqttOptions };
    }

    /**
     * 绑定 MQTT 事件
     */
    bindMqttEvents(connectTimeout) {
        const client = this.state.mqttClient;
        if (!client) return;

        client.on('connect', () => {
            clearTimeout(connectTimeout);
            this.onMqttConnect();
        });

        client.on('message', (topic, message) => {
            this.onMqttMessage(topic, message);
        });

        client.on('error', (error) => {
            clearTimeout(connectTimeout);
            this.onMqttError(error);
        });

        client.on('close', () => {
            clearTimeout(connectTimeout);
            this.onMqttClose();
        });

        client.on('reconnect', () => {
            this.logConnection('正在重连...', 'warning');
            this.updateMqttStatus('connecting', '重连中...');
        });

        client.on('offline', () => {
            this.logConnection('客户端离线', 'warning');
            this.updateMqttStatus('disconnected', '已离线');
        });
    }

    /**
     * 连接成功处理
     */
    onMqttConnect() {
        this.state.isConnecting = false;
        this.logConnection('监控连接已建立', 'success');
        this.updateMqttStatus('connected', '已连接');
        this.updateButtonState(true);
        
        // 自动订阅默认主题
        this.subscribeToTopic('/device/status');
        this.subscribeToTopic('task/assign');
        this.subscribeToTopic('task/result');
        this.subscribeToTopic('/device/list');

        this.showNotification('监控连接成功', 'success');
    }

    /**
     * 连接超时处理
     */
    handleConnectionTimeout() {
        this.state.isConnecting = false;
        this.logConnection('连接超时，请检查服务器地址和网络', 'error');
        this.updateMqttStatus('disconnected', '连接超时');
        this.updateButtonState(false);
        this.showNotification('连接超时，请检查配置', 'error');
        
        if (this.state.mqttClient) {
            this.state.mqttClient.end(true);
            this.state.mqttClient = null;
        }
    }

    /**
     * 连接错误处理
     */
    handleConnectionError(error) {
        this.state.isConnecting = false;
        this.logConnection(`监控连接异常: ${error.message}`, 'error');
        this.updateMqttStatus('disconnected', '连接异常');
        this.updateButtonState(false);
        this.showNotification(`连接异常: ${error.message}`, 'error');
        console.error('[MQTT Monitor] 连接错误:', error);
    }

    /**
     * 接收消息处理
     */
    onMqttMessage(topic, message) {
        this.state.mqttMessageCount++;
        this.updateMessageCount();
        
        const msgStr = message.toString();
        this.logReceivedMessage(topic, msgStr);
        
        // 尝试解析并处理系统消息
        try {
            const msgObj = JSON.parse(msgStr);
            this.processSystemMessage(topic, msgObj);
        } catch (e) {
            this.processNonJsonMessage(topic, msgStr);
        }
    }

    /**
     * 处理系统消息
     */
    processSystemMessage(topic, msgObj) {
        const clientIdMatch = topic.match(/sys\/clients\/([^\/]+)\/(status|connected)/);
        const clientId = clientIdMatch ? clientIdMatch[1] : null;
        
        if (!clientId) return;
        
        const isOnline = msgObj.status === 'online' || topic.includes('connected');
        const isOffline = msgObj.status === 'offline' || topic.includes('disconnected');
        
        if (isOnline) {
            this.showNotification(`设备 ${clientId} 已上线`, 'success');
        } else if (isOffline) {
            this.showNotification(`设备 ${clientId} 已下线`, 'warning');
        }
    }

    /**
     * 处理非 JSON 消息
     */
    processNonJsonMessage(topic, msgStr) {
        if (!topic.includes('sys/clients/')) return;
        
        const parts = topic.split('/');
        if (parts.length < 3) return;
        
        const clientId = parts[2];
        const lowerMsg = msgStr.toLowerCase();
        
        if (lowerMsg.includes('connected') || lowerMsg.includes('online')) {
            this.showNotification(`设备 ${clientId} 已连接`, 'success');
        } else if (lowerMsg.includes('disconnected') || lowerMsg.includes('offline')) {
            this.showNotification(`设备 ${clientId} 已断开`, 'warning');
        }
    }

    /**
     * MQTT 错误处理
     */
    onMqttError(error) {
        this.state.isConnecting = false;
        this.logConnection(`监控连接错误: ${error.message}`, 'error');
        this.updateMqttStatus('disconnected', '连接错误');
        this.updateButtonState(false);
        console.error('[MQTT Monitor] MQTT错误:', error);
    }

    /**
     * 连接关闭处理
     */
    onMqttClose() {
        this.state.isConnecting = false;
        this.logConnection('监控连接已关闭', 'warning');
        this.updateMqttStatus('disconnected', '已断开');
        this.updateButtonState(false);
    }

    /**
     * 断开 MQTT 连接
     */
    disconnectMqtt() {
        const client = this.state.mqttClient;
        if (!client) return;

        // 取消所有订阅
        this.state.currentSubscriptions.forEach(topic => {
            try {
                client.unsubscribe(topic);
            } catch (e) {
                console.warn(`取消订阅失败: ${topic}`, e);
            }
        });
        
        this.state.currentSubscriptions.clear();
        
        // 关闭连接
        try {
            client.end(true);
        } catch (e) {
            console.warn('关闭连接时出错:', e);
        }
        
        this.state.mqttClient = null;
        this.logConnection('已手动断开连接', 'info');
    }

    /**
     * 订阅主题
     */
    subscribeToTopic(topic = null) {
        const client = this.state.mqttClient;
        
        if (!client || client.disconnected) {
            this.logConnection('请先连接系统服务器', 'warning');
            return;
        }

        const topicToSubscribe = topic || this.elements.subscribeTopic?.value?.trim();
        
        if (!topicToSubscribe) {
            this.logConnection('请输入订阅主题', 'warning');
            return;
        }

        client.subscribe(topicToSubscribe, (err) => {
            if (err) {
                this.logConnection(`订阅失败: ${err.message}`, 'error');
            } else {
                this.state.currentSubscriptions.add(topicToSubscribe);
                this.logConnection(`成功订阅主题: ${topicToSubscribe}`, 'success');
            }
        });
    }

    /**
     * 取消订阅
     */
    unsubscribeFromTopic() {
        const client = this.state.mqttClient;
        
        if (!client || client.disconnected) {
            this.logConnection('请先连接系统服务器', 'warning');
            return;
        }

        const topic = this.elements.subscribeTopic?.value?.trim();
        
        if (!topic) {
            this.logConnection('请输入取消订阅的主题', 'warning');
            return;
        }

        client.unsubscribe(topic, (err) => {
            if (err) {
                this.logConnection(`取消订阅失败: ${err.message}`, 'error');
            } else {
                this.state.currentSubscriptions.delete(topic);
                this.logConnection(`成功取消订阅: ${topic}`, 'success');
            }
        });
    }

    /**
     * 发布消息
     */
    publishMessage() {
        const client = this.state.mqttClient;
        
        if (!client || client.disconnected) {
            this.logConnection('请先连接系统服务器', 'warning');
            return;
        }

        const topic = this.elements.publishTopic?.value?.trim();
        const payload = this.elements.publishPayload?.value;

        if (!topic) {
            this.logConnection('请输入主题', 'warning');
            return;
        }

        if (!payload) {
            this.logConnection('请输入消息内容', 'warning');
            return;
        }

        client.publish(topic, payload, (err) => {
            if (err) {
                this.logConnection(`发布消息失败: ${err.message}`, 'error');
            } else {
                this.logConnection(`消息已发布到: ${topic}`, 'success');
            }
        });
    }

    // ==================== UI 更新方法 ====================

    /**
     * 更新 MQTT 状态显示
     */
    updateMqttStatus(status, text) {
        const statusEl = this.elements.mqttStatus;
        if (!statusEl) return;
        
        statusEl.className = `status-badge status-${status}`;
        statusEl.textContent = text;
    }

    /**
     * 更新按钮状态
     */
    updateButtonState(connected) {
        if (this.elements.connectBtn) {
            this.elements.connectBtn.disabled = connected;
        }
        if (this.elements.disconnectBtn) {
            this.elements.disconnectBtn.disabled = !connected;
        }
    }

    /**
     * 更新消息计数
     */
    updateMessageCount() {
        if (this.elements.mqttMessageCount) {
            this.elements.mqttMessageCount.textContent = this.state.mqttMessageCount;
        }
        if (this.elements.deviceMessageCount) {
            this.elements.deviceMessageCount.textContent = this.state.mqttMessageCount;
        }
    }

    // ==================== 设备列表管理 ====================

    /**
     * 加载设备列表 - 使用新的 MonitorController 接口
     */
    async loadClients() {
        try {
            this.showLoading(true);
            
            // 并行获取节点信息和在线设备列表
            const [nodeInfoResponse, aliveResponse] = await Promise.all([
                fetch(`${this.config.monitorApiUrl}/nodeInfo`, {
                    method: 'GET',
                    headers: {
                        'Accept': 'application/json'
                    }
                }),
                fetch(`${this.config.monitorApiUrl}/alive`, {
                    method: 'GET',
                    headers: {
                        'Accept': 'application/json'
                    }
                })
            ]);

            if (!nodeInfoResponse.ok) {
                throw new Error(`NodeInfo HTTP ${nodeInfoResponse.status}: ${nodeInfoResponse.statusText}`);
            }
            if (!aliveResponse.ok) {
                throw new Error(`Alive HTTP ${aliveResponse.status}: ${aliveResponse.statusText}`);
            }

            const nodeInfoData = await nodeInfoResponse.json();
            const aliveData = await aliveResponse.json();
            
            this.processMonitorData(nodeInfoData, aliveData);
        } catch (error) {
            this.logConnection(`获取设备失败: ${error.message}`, 'error');
            this.showError(`获取设备列表失败: ${error.message}`);
        } finally {
            this.showLoading(false);
        }
    }

    /**
     * 处理监控数据 - 整合 NodeInfo 和 Alive 数据
     */
    processMonitorData(nodeInfoData, aliveData) {
        // 存储节点信息
        this.state.nodeInfos = nodeInfoData || {};
        
        // 存储在线设备列表
        this.state.aliveDevices = new Set(aliveData || []);
        
        // 将节点信息转换为客户端列表格式
        const clients = this.convertNodeInfoToClients(this.state.nodeInfos, this.state.aliveDevices);
        
        this.detectStatusChanges(clients);
        this.state.clients = clients;
        this.filterClients();
        this.updateStats();
        this.updateLastUpdateTime();
    }

    /**
     * 将 NodeInfo 数据转换为客户端格式
     */
    convertNodeInfoToClients(nodeInfos, aliveDevices) {
        const clients = [];
        
        Object.entries(nodeInfos).forEach(([deviceName, nodeInfo]) => {
            const isOnline = aliveDevices.has(deviceName);
            
            clients.push({
                clientId: deviceName,
                deviceName: deviceName,
                connected: isOnline,
                // NodeInfo 详细指标
                cpuUsage: nodeInfo.cpuUsage || 0,
                memoryUsage: nodeInfo.memoryUsage || 0,
                powerRemain: nodeInfo.powerRemain || 0,
                storageRemain: nodeInfo.storageRemain || 0,
                latency: nodeInfo.latency || 0,
                // 保留原有字段以兼容
                username: 'N/A',
                ipAddress: 'N/A',
                port: 'N/A',
                connectedAt: Date.now(),
                protoName: 'MQTT',
                protoVer: '3.1.1'
            });
        });
        
        // 如果没有任何节点信息，显示空列表
        return clients;
    }

    /**
     * 检测设备状态变化
     */
    detectStatusChanges(currentClients) {
        const currentMap = new Map(currentClients.map(c => [c.clientId, c]));
        const prevMap = this.state.previousClients;
        
        // 检测新设备和状态变化
        currentClients.forEach(client => {
            const prevClient = prevMap.get(client.clientId);
            
            if (!prevClient) {
                this.showNotification(`设备 ${client.clientId} 已连接`, 'success');
            } else if (!prevClient.connected && client.connected) {
                this.showNotification(`设备 ${client.clientId} 已上线`, 'success');
            } else if (prevClient.connected && !client.connected) {
                this.showNotification(`设备 ${client.clientId} 已下线`, 'warning');
            }
        });
        
        // 检测断开连接的设备
        prevMap.forEach((prevClient, clientId) => {
            if (!currentMap.has(clientId)) {
                this.showNotification(`设备 ${clientId} 已断开连接`, 'warning');
            }
        });
        
        this.state.previousClients = currentMap;
    }

    /**
     * 筛选设备
     */
    filterClients() {
        const searchTerm = this.elements.searchInput?.value.toLowerCase().trim() || '';
        const statusFilter = this.elements.statusFilter?.value || '';

        this.state.filteredClients = this.state.clients.filter(client => {
            const matchesSearch = !searchTerm || 
                client.clientId.toLowerCase().includes(searchTerm) ||
                (client.username && client.username.toLowerCase().includes(searchTerm));
            
            const matchesStatus = !statusFilter || 
                client.connected.toString() === statusFilter;

            return matchesSearch && matchesStatus;
        });

        this.renderClients();
        this.updateStats();
    }

    /**
     * 渲染设备列表
     */
    renderClients() {
        const container = this.elements.clientList;
        if (!container) return;

        if (this.state.filteredClients.length === 0) {
            container.innerHTML = '<div class="no-data">没有找到匹配的设备</div>';
            return;
        }

        const fragment = document.createDocumentFragment();
        
        this.state.filteredClients.forEach(client => {
            const card = this.createClientCardElement(client);
            fragment.appendChild(card);
        });

        container.innerHTML = '';
        container.appendChild(fragment);
    }

    /**
     * 创建设备卡片元素 - 显示详细的节点信息
     */
    createClientCardElement(client) {
        const statusClass = client.connected ? 'status-online' : 'status-offline';
        const statusText = client.connected ? '在线' : '离线';
        const cardClass = client.connected ? 'device-card online' : 'device-card offline';
        
        // 格式化数值显示
        const formatPercent = (val) => `${((val || 0) * 1).toFixed(2)}%`;
        // 存储返回的是百分比小数（如 0.9473 表示 94.73%）
        const formatStorage = (val) => `${((val || 0) * 1).toFixed(2)}%`;
        // 延迟返回的是秒，转换为毫秒
        const formatLatency = (val) => `${((val || 0) * 1).toFixed(2)} ms`;
        
        // 根据使用率确定颜色等级
        const getUsageLevel = (val) => {
            if (val >= 80) return 'critical';
            if (val >= 60) return 'warning';
            return 'normal';
        };
        
        const cpuLevel = getUsageLevel(client.cpuUsage);
        const memLevel = getUsageLevel(client.memoryUsage);
        const batteryLevel = client.powerRemain <= 20 ? 'critical' : client.powerRemain <= 50 ? 'warning' : 'normal';
        
        const card = document.createElement('article');
        card.className = cardClass;
        card.setAttribute('role', 'article');
        card.setAttribute('aria-label', `设备 ${client.clientId}, 状态: ${statusText}`);
        card.setAttribute('tabindex', '0');
        
        card.innerHTML = `
            <header class="device-header">
                <span class="device-id">${this.escapeHtml(client.clientId)}</span>
                <div class="device-status" aria-label="状态: ${statusText}">
                    <span class="status-dot ${statusClass}" aria-hidden="true"></span>
                    <span class="status-text">${statusText}</span>
                </div>
            </header>
            <div class="device-info">
                <!-- CPU使用率 -->
                <div class="metric-row">
                    <div class="metric-label">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <rect x="4" y="4" width="16" height="16" rx="2" ry="2"/>
                            <rect x="9" y="9" width="6" height="6"/>
                            <line x1="9" y1="1" x2="9" y2="4"/>
                            <line x1="15" y1="1" x2="15" y2="4"/>
                            <line x1="9" y1="20" x2="9" y2="23"/>
                            <line x1="15" y1="20" x2="15" y2="23"/>
                            <line x1="20" y1="9" x2="23" y2="9"/>
                            <line x1="20" y1="14" x2="23" y2="14"/>
                            <line x1="1" y1="9" x2="4" y2="9"/>
                            <line x1="1" y1="14" x2="4" y2="14"/>
                        </svg>
                        CPU
                    </div>
                    <div class="metric-bar">
                        <div class="metric-progress ${cpuLevel}" style="width: ${Math.min(client.cpuUsage || 0, 100)}%"></div>
                    </div>
                    <span class="metric-value ${cpuLevel}">${formatPercent(client.cpuUsage)}</span>
                </div>
                
                <!-- 内存使用率 -->
                <div class="metric-row">
                    <div class="metric-label">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <rect x="2" y="2" width="20" height="20" rx="2" ry="2"/>
                            <line x1="2" y1="10" x2="22" y2="10"/>
                            <line x1="6" y1="6" x2="6.01" y2="6"/>
                            <line x1="10" y1="6" x2="10.01" y2="6"/>
                            <line x1="14" y1="6" x2="14.01" y2="6"/>
                            <line x1="18" y1="6" x2="18.01" y2="6"/>
                        </svg>
                        内存
                    </div>
                    <div class="metric-bar">
                        <div class="metric-progress ${memLevel}" style="width: ${Math.min(client.memoryUsage || 0, 100)}%"></div>
                    </div>
                    <span class="metric-value ${memLevel}">${formatPercent(client.memoryUsage)}</span>
                </div>
                
                <!-- 电池电量 -->
                <div class="metric-row">
                    <div class="metric-label">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <rect x="2" y="7" width="16" height="10" rx="2" ry="2"/>
                            <line x1="22" y1="11" x2="22" y2="13"/>
                            <line x1="6" y1="11" x2="6" y2="13"/>
                        </svg>
                        电量
                    </div>
                    <div class="metric-bar">
                        <div class="metric-progress battery ${batteryLevel}" style="width: ${Math.min(client.powerRemain || 0, 100)}%"></div>
                    </div>
                    <span class="metric-value ${batteryLevel}">${formatPercent(client.powerRemain)}</span>
                </div>
                
                <!-- 存储空间 -->
                <div class="metric-row">
                    <div class="metric-label">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                            <polyline points="7 10 12 15 17 10"/>
                            <line x1="12" y1="15" x2="12" y2="3"/>
                        </svg>
                        存储
                    </div>
                    <span class="metric-value storage">${formatStorage(client.storageRemain || 0)}</span>
                </div>
                
                <!-- 网络延迟 -->
                <div class="metric-row">
                    <div class="metric-label">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
                        </svg>
                        延迟
                    </div>
                    <span class="metric-value latency ${client.latency > 200 ? 'warning' : client.latency > 500 ? 'critical' : 'normal'}">${formatLatency(client.latency)}</span>
                </div>
            </div>
        `;
        
        return card;
    }

    /**
     * 更新统计数据
     */
    updateStats() {
        const clients = this.state.clients;
        const filtered = this.state.filteredClients;
        
        const total = clients.length;
        const online = clients.filter(c => c.connected).length;
        const offline = total - online;

        if (this.elements.totalClients) {
            this.elements.totalClients.textContent = total;
        }
        if (this.elements.onlineClients) {
            this.elements.onlineClients.textContent = online;
        }
        if (this.elements.offlineClients) {
            this.elements.offlineClients.textContent = offline;
        }
    }

    // ==================== 日志管理 ====================

    /**
     * 记录连接日志
     */
    logConnection(message, type = 'info') {
        const container = this.elements.connectionLog;
        if (!container) return;

        const entry = document.createElement('div');
        entry.className = `log-entry log-${type}`;
        entry.innerHTML = `
            <time class="log-time" datetime="${new Date().toISOString()}">${new Date().toLocaleTimeString()}</time>
            <span class="log-content">${this.escapeHtml(message)}</span>
        `;

        container.appendChild(entry);
        container.scrollTop = container.scrollHeight;
        
        // 限制日志条数
        this.limitLogEntries(container, this.config.maxLogEntries);
        
        this.state.connectionLogCount++;
    }

    /**
     * 记录接收到的消息
     */
    logReceivedMessage(topic, payload) {
        const container = this.elements.receivedMessages;
        if (!container) return;

        // 移除空状态提示
        const noData = container.querySelector('.no-data');
        if (noData) {
            noData.remove();
        }

        const entry = document.createElement('div');
        entry.className = 'message-item';
        entry.innerHTML = `
            <div>
                <span class="message-topic">${this.escapeHtml(topic)}</span>
                <time class="message-timestamp">${new Date().toLocaleTimeString()}</time>
            </div>
            <div class="message-payload">${this.escapeHtml(payload)}</div>
        `;

        container.appendChild(entry);
        container.scrollTop = container.scrollHeight;
        
        // 限制消息条数
        this.limitLogEntries(container, this.config.maxMessageEntries);
    }

    /**
     * 限制日志条目数量
     */
    limitLogEntries(container, maxEntries) {
        while (container.children.length > maxEntries) {
            container.removeChild(container.firstChild);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 显示加载状态
     */
    showLoading(show) {
        const loadingEl = document.getElementById('loading');
        if (loadingEl) {
            loadingEl.classList.toggle('hidden', !show);
        }
    }

    /**
     * 显示错误信息
     */
    showError(message) {
        const errorEl = document.getElementById('error');
        if (errorEl) {
            errorEl.textContent = message;
            errorEl.classList.remove('hidden');
            
            setTimeout(() => {
                errorEl.classList.add('hidden');
            }, 5000);
        }
    }

    /**
     * 显示通知
     */
    showNotification(message, type = 'info') {
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.setAttribute('role', 'alert');
        notification.textContent = message;
        
        document.body.appendChild(notification);
        
        // 自动移除
        setTimeout(() => {
            notification.style.animation = 'slideOutRight 0.3s ease';
            setTimeout(() => notification.remove(), 300);
        }, 3000);
    }

    /**
     * 更新最后更新时间
     */
    updateLastUpdateTime() {
        const now = new Date();
        const timeStr = now.toLocaleTimeString('zh-CN');
        
        if (this.elements.lastUpdate) {
            this.elements.lastUpdate.textContent = `上次更新: ${timeStr}`;
            this.elements.lastUpdate.setAttribute('datetime', now.toISOString());
        }
    }

    /**
     * 启动自动刷新
     */
    startAutoRefresh() {
        setInterval(() => {
            this.loadClients();
        }, this.config.autoRefreshInterval);
    }

    /**
     * HTML 转义
     */
    escapeHtml(text) {
        if (typeof text !== 'string') return String(text);
        
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// ==================== 初始化 ====================

document.addEventListener('DOMContentLoaded', () => {
    // 检查MQTT库
    if (typeof mqtt === 'undefined') {
        console.error('[MQTT Monitor] MQTT库未加载');
        document.body.innerHTML = `
            <div style="padding: 40px; text-align: center; font-family: system-ui, sans-serif;">
                <h2 style="color: #dc2626; margin-bottom: 16px;">⚠️ 系统监控库加载失败</h2>
                <p style="color: #6b7280;">请检查网络连接后刷新页面重试</p>
                <button onclick="location.reload()" style="margin-top: 20px; padding: 10px 20px; cursor: pointer;">
                    刷新页面
                </button>
            </div>
        `;
        return;
    }
    
    // 初始化监控器
    window.mqttMonitor = new MqttJsMonitor();
});

// 添加 slideOutRight 动画样式
const style = document.createElement('style');
style.textContent = `
    @keyframes slideOutRight {
        from { transform: translateX(0); opacity: 1; }
        to { transform: translateX(100%); opacity: 0; }
    }
`;
document.head.appendChild(style);
