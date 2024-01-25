package pers.hanwei.ulearn.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pers.hanwei.ulearn.media.mapper.MediaFilesMapper;
import pers.hanwei.ulearn.media.mapper.MediaProcessHistoryMapper;
import pers.hanwei.ulearn.media.mapper.MediaProcessMapper;
import pers.hanwei.ulearn.media.model.po.MediaFiles;
import pers.hanwei.ulearn.media.model.po.MediaProcess;
import pers.hanwei.ulearn.media.model.po.MediaProcessHistory;
import pers.hanwei.ulearn.media.service.MediaFileProcessService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class MediaFileProcessServiceImpl implements MediaFileProcessService {
    @Autowired
    MediaProcessMapper mediaProcessMapper;

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MediaProcessHistoryMapper mediaProcessHistoryMapper;
    /**
     * 获取待处理任务列表
     * @param shardIndex 分片序号——执行器序号
     * @param shardTotal 分片总数——执行器总数
     * @param count      获取记录数
     * @return           待处理任务列表
     */
    @Override
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count) {
        return mediaProcessMapper.selectListByShardIndex(shardTotal, shardIndex, count);
    }

    /**
     * 更新待处理任务状态：更新相关状态、从待处理事件中删除该记录、添加记录到处理历史中
     * @param taskId 任务id
     * @param status 任务状态：1:未处理，2：处理成功  3：处理失败
     * @param fileId 文件id
     * @param url    资源地址
     * @param errorMsg 错误信息
     */
    @Override
    @Transactional
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {
        // 查找任务
        MediaProcess task = mediaProcessMapper.selectById(taskId);
        if(task == null) return ;

        // 处理失败
        if(status.equals("3")) {
            MediaProcess updateInfo = new MediaProcess();
            updateInfo.setStatus(status);
            updateInfo.setErrormsg(errorMsg);
            updateInfo.setFailCount(task.getFailCount() + 1);
            mediaProcessMapper.update(updateInfo, new LambdaQueryWrapper<MediaProcess>().eq(MediaProcess::getId, taskId));
            log.error("更新任务处理状态为失败，任务信息:{}", updateInfo);
        }

        // 处理成功: 根据文件id找到媒体文件记录->更新文件记录的资源地址->更新任务记录
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        if(mediaFiles!=null){
            //更新媒资文件中的访问url
            mediaFiles.setUrl(url);
            mediaFilesMapper.updateById(mediaFiles);
        }

        // 添加到历史记录
        task.setUrl(url);
        task.setStatus("2");
        task.setFinishDate(LocalDateTime.now());
        MediaProcessHistory finishedTask = new MediaProcessHistory();
        BeanUtils.copyProperties(task, finishedTask);
        mediaProcessHistoryMapper.insert(finishedTask);

        // 删除任务表记录
        mediaProcessMapper.deleteById(taskId);
    }

    /**
     * 争抢任务
     * @param id 任务Id
     * @return 是否抢到任务
     */
    @Override
    public boolean startTask(Long id) {
        return mediaProcessMapper.startTask(id) > 0;
    }


}
