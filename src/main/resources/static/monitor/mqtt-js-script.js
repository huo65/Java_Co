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
            mqttClient: null,
            mqttMessageCount: 0,
            connectionLogCount: 0,
            isConnecting: false,
            currentSubscriptions: new Set()
        };
        
        // 生成客户端ID
        this.clientId = `web-monitor-${Date.now()}`;
        
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
        this.subscribeToTopic('sys/clients/+/status');
        
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
     * 加载设备列表
     */
    async loadClients() {
        try {
            this.showLoading(true);
            
            const response = await fetch(
                `${this.config.apiUrl}/clients?_page=1&_limit=10000`,
                {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Basic ' + btoa(this.config.username + ':' + this.config.password),
                        'Accept': 'application/json'
                    }
                }
            );

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();
            
            if (data.code === 1) {
                this.processClientsData(data);
            } else {
                throw new Error(`API错误: ${data.code}`);
            }
        } catch (error) {
            this.logConnection(`获取设备失败: ${error.message}`, 'error');
            this.showError(`获取设备列表失败: ${error.message}`);
        } finally {
            this.showLoading(false);
        }
    }

    /**
     * 处理设备数据
     */
    processClientsData(data) {
        let clients = [];
        if (data.data?.list) {
            clients = data.data.list;
        } else if (Array.isArray(data.data)) {
            clients = data.data;
        }
        
        const normalizedClients = clients.map(client => ({
            clientId: client.clientId || 'unknown',
            username: client.username || 'N/A',
            connected: Boolean(client.connected),
            ipAddress: client.ipAddress || 'N/A',
            port: client.port || 'N/A',
            connectedAt: client.connectedAt || client.createdAt || Date.now(),
            protoName: client.protoName || 'N/A',
            protoVer: client.protoVer || 'N/A'
        }));
        
        this.detectStatusChanges(normalizedClients);
        this.state.clients = normalizedClients;
        this.filterClients();
        this.updateStats();
        this.updateLastUpdateTime();
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
     * 创建设备卡片元素
     */
    createClientCardElement(client) {
        const statusClass = client.connected ? 'status-online' : 'status-offline';
        const statusText = client.connected ? '在线' : '离线';
        const connectedAt = new Date(client.connectedAt).toLocaleString('zh-CN');
        
        const card = document.createElement('article');
        card.className = 'device-card';
        card.setAttribute('role', 'article');
        card.setAttribute('aria-label', `设备 ${client.clientId}`);
        card.setAttribute('tabindex', '0');
        
        card.innerHTML = `
            <header class="device-header">
                <span class="device-id">${this.escapeHtml(client.clientId)}</span>
                <div class="device-status" aria-label="状态: ${statusText}">
                    <span class="status-dot ${statusClass}" aria-hidden="true"></span>
                    <span>${statusText}</span>
                </div>
            </header>
            <div class="device-info">
                <div class="info-row">
                    <span class="info-label">IP地址</span>
                    <span class="info-value">${this.escapeHtml(client.ipAddress)}:${client.port}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">协议版本</span>
                    <span class="info-value">${client.protoName} ${client.protoVer}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">连接时间</span>
                    <time class="info-value" datetime="${new Date(client.connectedAt).toISOString()}">${connectedAt}</time>
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
