package cn.huo.ohmqttserver.optimization;

import lombok.Data;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

/**
 * 1. @ClassName TaskInfo
 * 2. @Description 任务信息解析
 * 3. @Author huo
 * 4. @Date 2025/11/19 下午6:54
 */
@Data
@Component
public class TaskInfo {
    private String taskId;
    private String fromClient;
    private String toClient;
    private BigInteger StartTimestamp;
    private BigInteger EndTimestamp;

    public static TaskInfo parseTaskInfo(String taskMessage){
        JSONObject taskInfoJSON = new JSONObject(taskMessage);
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setTaskId(taskInfoJSON.getString("taskId"));
        taskInfo.setFromClient(taskInfoJSON.getString("fromClient"));
        taskInfo.setToClient(taskInfoJSON.getString("toClient"));
        taskInfo.setStartTimestamp(taskInfoJSON.getBigInteger("timestamp"));

        return taskInfo;
    }

    public static TaskInfo parseResultInfo(String resultMessage) {
        JSONObject taskInfoJSON = new JSONObject(resultMessage);
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setTaskId(taskInfoJSON.getString("taskId"));
        taskInfo.setFromClient(taskInfoJSON.getString("fromClient"));
        taskInfo.setToClient(taskInfoJSON.getString("toClient"));
        taskInfo.setEndTimestamp(taskInfoJSON.getBigInteger("timestamp"));

        return taskInfo;
    }
}
