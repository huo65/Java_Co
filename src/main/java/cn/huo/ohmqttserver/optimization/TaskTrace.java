package cn.huo.ohmqttserver.optimization;

import cn.huo.ohmqttserver.optimization.dao.TaskSample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1. @ClassName TaskTrace
 * 2. @Description Task监控
 * 3. @Author huo
 * 4. @Date 2025/11/19 下午8:37
 */

public class TaskTrace {

    public static final Map<String,TaskSample> taskWithoutResult = new HashMap<>();
    public static final Map<String,String> taskStartTime = new HashMap<>();

//    TODO 后续拓展任务追踪
}
