package cn.huo.ohmqttserver.optimization.training;

import cn.huo.ohmqttserver.optimization.dto.RegressionModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * 模型缓存
 * 使用Caffeine-like机制缓存训练好的模型
 */
@Slf4j
@Component
public class ModelCache {

    private volatile CachedModelEntry cachedEntry;
    private final Object lock = new Object();

    /**
     * 默认缓存过期时间（分钟）
     */
    private static final int DEFAULT_EXPIRE_MINUTES = 30;

    @PostConstruct
    public void init() {
        log.info("模型缓存组件初始化完成");
    }

    /**
     * 获取缓存的模型
     *
     * @return 缓存的模型，如果过期或不存在则返回null
     */
    public RegressionModel getCachedModel() {
        CachedModelEntry entry = cachedEntry;
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            log.debug("模型缓存已过期");
            return null;
        }

        log.debug("命中模型缓存，版本: {}", entry.getModel().getVersion());
        return entry.getModel();
    }

    /**
     * 缓存模型
     *
     * @param model 要缓存的模型
     */
    public void cacheModel(RegressionModel model) {
        cacheModel(model, DEFAULT_EXPIRE_MINUTES);
    }

    /**
     * 缓存模型（带过期时间）
     *
     * @param model         要缓存的模型
     * @param expireMinutes 过期时间（分钟）
     */
    public void cacheModel(RegressionModel model, int expireMinutes) {
        if (model == null || !model.isValid()) {
            log.warn("尝试缓存无效模型，已忽略");
            return;
        }

        synchronized (lock) {
            cachedEntry = new CachedModelEntry(model, expireMinutes);
            log.info("模型已缓存，版本: {}，过期时间: {}分钟", model.getVersion(), expireMinutes);
        }
    }

    /**
     * 使缓存失效
     */
    public void invalidate() {
        synchronized (lock) {
            if (cachedEntry != null) {
                log.info("模型缓存已失效，版本: {}", cachedEntry.getModel().getVersion());
                cachedEntry = null;
            }
        }
    }

    /**
     * 检查缓存是否有效
     *
     * @return 缓存是否有效
     */
    public boolean isValid() {
        CachedModelEntry entry = cachedEntry;
        return entry != null && !entry.isExpired() && entry.getModel().isValid();
    }

    /**
     * 获取缓存元数据
     *
     * @return 缓存信息字符串
     */
    public String getCacheInfo() {
        CachedModelEntry entry = cachedEntry;
        if (entry == null) {
            return "无缓存";
        }

        RegressionModel model = entry.getModel();
        return String.format("版本: %s, 训练时间: %s, 样本数: %d, R²: %.4f, %s",
                model.getVersion(),
                model.getTrainedAt(),
                model.getSampleCount(),
                model.getRSquared(),
                entry.isExpired() ? "已过期" : "有效");
    }

    /**
     * 缓存条目
     */
    private static class CachedModelEntry {
        private final RegressionModel model;
        private final LocalDateTime cachedAt;
        private final int expireMinutes;

        CachedModelEntry(RegressionModel model, int expireMinutes) {
            this.model = model;
            this.cachedAt = LocalDateTime.now();
            this.expireMinutes = expireMinutes;
        }

        RegressionModel getModel() {
            return model;
        }

        boolean isExpired() {
            return ChronoUnit.MINUTES.between(cachedAt, LocalDateTime.now()) > expireMinutes;
        }
    }
}
