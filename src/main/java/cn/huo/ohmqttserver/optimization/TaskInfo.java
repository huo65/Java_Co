package cn.huo.ohmqttserver.optimization;

import lombok.Data;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * 1. @ClassName TaskInfo
 * 2. @Description 任务信息解析
 * 3. @Author huo
 * 4. @Date 2025/11/19 下午6:54
 */
@Data
@Component
public class TaskInfo {
//    {"taskId":"1747991732356","fromClient":"OpenHarmony 3.2","toClient":"Lenovo TB-J607F","timestamp":"now","params":{"bitmap":}}
    private String taskId;
    private String fromClient;
    private String toClient;
    private String StartTimestamp;
    private String EndTimestamp;
//    private TaskParams params;

    public static TaskInfo parseTaskInfo(String taskMessage){
        JSONObject taskInfoJSON = new JSONObject(taskMessage);
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setTaskId(taskInfoJSON.getString("taskId"));
        taskInfo.setFromClient(taskInfoJSON.getString("fromClient"));
        taskInfo.setToClient(taskInfoJSON.getString("toClient"));
        taskInfo.setStartTimestamp(taskInfoJSON.getString("timestamp"));

        return taskInfo;
    }

    public static TaskInfo parseResultInfo(String resultMessage) {
        JSONObject taskInfoJSON = new JSONObject(resultMessage);
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setTaskId(taskInfoJSON.getString("taskId"));
        taskInfo.setFromClient(taskInfoJSON.getString("fromClient"));
        taskInfo.setToClient(taskInfoJSON.getString("toClient"));
        taskInfo.setEndTimestamp(taskInfoJSON.getString("timestamp"));

        return taskInfo;
    }
}
