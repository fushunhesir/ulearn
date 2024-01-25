package pers.hanwei.ulearn.media.service.jobhandler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pers.hanwei.ulearn.base.utils.Mp4VideoUtil;
import pers.hanwei.ulearn.media.model.po.MediaProcess;
import pers.hanwei.ulearn.media.service.MediaFileProcessService;
import pers.hanwei.ulearn.media.service.MediaFileService;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class VideoTask {
    private final String ffmpegPath = "/opt/homebrew/bin/ffmpeg";

    @Autowired
    MediaFileProcessService mediaFileProcessService;

    @Autowired
    MediaFileService mediaFileService;

    /**
     * 视频处理任务：将视频转码
     */
    @XxlJob("videoJobHandler")
    public void videoHandler() throws Exception {
        // 分布式任务调度——获取本节点的参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.debug("切片索引号: {}, 切片总数: {}", shardIndex, shardTotal);

        // 配置分布式处理参数——多少个线程
        int coreNum = Runtime.getRuntime().availableProcessors();
        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, coreNum);
        int threadNum = mediaProcessList.size();
        log.debug("待处理任务数为:{}", threadNum);
        if(threadNum == 0) return ;

        // 设置线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(threadNum);
        // 设置定时器——用于防止无限等待
        CountDownLatch countDownLatch = new CountDownLatch(threadNum);
        // 设置并启动线程转码任务逻辑
        mediaProcessList.forEach(mediaProcess -> {
            threadPool.execute(()-> {
                // 获取/争抢任务
                Long taskId = mediaProcess.getId();
                boolean success = mediaFileProcessService.startTask(taskId);
                if(!success) return ;

                // 转码文件逻辑
                log.debug("现在开始转码文件:{}", mediaProcess);
                if(!videoTrancoding(mediaProcess, countDownLatch)) {
                    log.error("转码文件失败");
                }
            });
        });
        countDownLatch.await(30, TimeUnit.MINUTES);
    }


    /**
     * 转码视频文件为MP4文件
     * @param task 任务记录——包括文件ID等信息
     * @param countDownLatch 定时器——避免无限等待
     * @return 是否成功转码
     */
    private boolean videoTrancoding(MediaProcess task, CountDownLatch countDownLatch) {
        try {
            // 从minio下载文件
            //原始视频的md5值
            String fileId = task.getFileId();
            //原始文件名称
            String filename = task.getFilename();
            //将要处理的文件下载到服务器上
            File originalFile = mediaFileService.downloadFileFromMinIO(task.getBucket(), task.getFilePath());
            if (originalFile == null) {
                log.debug("下载待处理文件失败,originalFile:{}", task.getBucket().concat(task.getFilePath()));
                mediaFileProcessService.saveProcessFinishStatus(task.getId(), "3", fileId, null, "下载待处理文件失败");
                return false;
            }

            /*
            转码下载的文件
             */
            // 创建临时文件
            File tempMP4File = null;
            try {
                tempMP4File = File.createTempFile("mp4", ".mp4");
            } catch (Exception e) {
                e.printStackTrace();
                log.error("转码处理：创建临时文件失败");
                return false;
            }
            // 利用工具类转码
            String result = "";
            try {
                Mp4VideoUtil util = new Mp4VideoUtil(ffmpegPath,
                        originalFile.getAbsolutePath(),
                        tempMP4File.getName(),
                        tempMP4File.getAbsolutePath());
                result = util.generateMp4();
            } catch (Exception e) {
                e.printStackTrace();
                log.error("转码处理：转码文件失败");
                return false;
            }
            if (!result.equals("success")) {
                log.error("视频转码失败，视频地址为:{}，日志信息为：{}",
                        task.getBucket() + task.getFilePath(),
                        result);
                // 更新任务信息
                mediaFileProcessService.saveProcessFinishStatus(task.getId(),
                        "3", fileId, null, result);
            }

            /*
            存储转码后的文件
             */
            // 配置存储至minio的参数
            String objectName = getFilePath(fileId, ".mp4");

            // 将临时文件存储
            try {
                mediaFileService.uploadFileToMinio(tempMP4File.getAbsolutePath(),
                        objectName,
                        "video/mp4",
                        task.getBucket());
            } catch (Exception e) {
                log.error("上传至MINIO失败");
            }

            // 保存、更新信息
            String url = "/" + task.getBucket() + "/" + objectName;
            mediaFileProcessService.saveProcessFinishStatus(task.getId(), "2", fileId, url, null);

            return true;
        } finally {
            countDownLatch.countDown();
        }
    }

    /**
     * 生成MINIO对象路径
     * @param fileMd5 md5值
     * @param fileExt 文件扩展名
     * @return MINIO对象路径
     */
    private String getFilePath(String fileMd5,String fileExt){
        return   fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" +fileMd5 +fileExt;
    }
}
