package pers.hanwei.ulearn.media.service;

import pers.hanwei.ulearn.media.model.po.MediaProcess;

import java.util.List;

public interface MediaFileProcessService {
    /**
     * 获取待处理任务
     * @param shardIndex 分片序号——执行器序号
     * @param shardTotal 分片总数——执行器总数
     * @param count      获取记录数
     * @return           待处理任务列表
     */
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count);

    /**
     * 任务完成后更新任务状态
     * @param taskId 任务id
     * @param status 任务状态：1:未处理，2：处理成功  3：处理失败
     * @param fileId 文件id
     * @param url    资源地址
     * @param errorMsg 错误信息
     */
    void saveProcessFinishStatus(Long taskId,String status,String fileId,String url,String errorMsg);

    /**
     * 启动/争抢任务
     * @param id 任务Id
     * @return 是否争抢成功
     */
    boolean startTask(Long id);
}
