package pers.hanwei.ulearn.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import pers.hanwei.ulearn.base.exception.ULearnException;
import pers.hanwei.ulearn.base.model.PageParams;
import pers.hanwei.ulearn.base.model.PageResult;
import pers.hanwei.ulearn.base.model.RestResponse;
import pers.hanwei.ulearn.media.mapper.MediaFilesMapper;
import pers.hanwei.ulearn.media.mapper.MediaProcessMapper;
import pers.hanwei.ulearn.media.model.UploadFileParams;
import pers.hanwei.ulearn.media.model.dto.QueryMediaParamsDto;
import pers.hanwei.ulearn.media.model.dto.UploadFileResultDto;
import pers.hanwei.ulearn.media.model.po.MediaFiles;
import pers.hanwei.ulearn.media.model.po.MediaProcess;
import pers.hanwei.ulearn.media.service.MediaFileService;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mr.M
 * @version 1.0
 * @description meishenme
 * @date 2022/9/10 8:58
 */
@Service
@Slf4j
public class MediaFileServiceImpl implements MediaFileService {

    //存储普通文件
    @Value("${minio.bucket.files}")
    private String bucket_mediafiles;

    //存储视频
    @Value("${minio.bucket.videofiles}")
    private String bucket_video;

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MediaProcessMapper mediaProcessMapper;

    @Autowired
    MinioClient client;

    @Autowired
    MediaFileService currentProxy;

    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParams params, String path) {
        // 将临时文件写入minio
        File file = new File(path);
        if (!file.exists()) {
            throw new ULearnException("File not exist");
        }
        /**
         * 思考为什么需要mimeType、上传数据到minio需要哪些参数，如何生成md5，如何根据extension生成MIME类型
         */
        String extension = params.getFilename().substring(params.getFilename().lastIndexOf('.'));
        String md5 = getMd5(file);
        String name = genMinioPath() + md5 + extension;
        String mimeType = getMIMEType(extension);
        if (!uploadFileToMinio(path, name, mimeType, bucket_mediafiles)) {
            return null;
        }

        // 将文件信息写入media表中
        MediaFiles data = updateMediaDB(companyId, name, params, md5, bucket_mediafiles);
        UploadFileResultDto dto = new UploadFileResultDto();
        BeanUtils.copyProperties(data, dto);
        return dto;
    }

    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles != null) {
            String bucket = mediaFiles.getBucket();
            String filePath = mediaFiles.getFilePath();
            InputStream stream = null;
            try {
                stream = client.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucket)
                                .object(filePath)
                                .build());

                if (stream != null) {
                    //文件已存在
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
            }
        }
        return RestResponse.success(false);
    }

    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //得到分块文件的路径
        String chunkFilePath = chunkFileFolderPath + chunkIndex;

        //文件流
        InputStream fileInputStream = null;
        try {
            fileInputStream = client.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket_video)
                            .object(chunkFilePath)
                            .build());

            if (fileInputStream != null) {
                //分块已存在
                return RestResponse.success(true);
            }
        } catch (Exception e) {

        }
        //分块未存在
        return RestResponse.success(false);
    }

    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {
        //得到分块文件的目录路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //得到分块文件的路径
        String chunkFilePath = chunkFileFolderPath + chunk;

        try {
            //将文件存储至minIO
            uploadFileToMinio(localChunkFilePath, chunkFilePath, getMIMEType(null), bucket_video);
            return RestResponse.success(true);
        } catch (Exception ex) {
        }
        return RestResponse.validfail(false,"上传分块失败");
    }

    /**
     * 合并分块
     * @param fileMd5 文件的md5值
     * @param uploadFileParams 数据库将要存储的文件名称
     * @param chunkTotal 分块总数
     * @return 成功与否
     */
    @Override
    public RestResponse mergeChunk(Long companyId, String fileMd5, int chunkTotal, UploadFileParams uploadFileParams) {
        /*
         找到所有分块
         */
        // 获取minio中对象的名字
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        String fileName = uploadFileParams.getFilename();
        // 找到所有分块文件
        List<ComposeSource> sourceObjectList = Stream.iterate(0, i -> ++i)
                .limit(chunkTotal)
                .map(i -> ComposeSource.builder()
                        .bucket(bucket_video)
                        .object(chunkFileFolderPath.concat(Integer.toString(i)))
                        .build()).collect(Collectors.toList());

        // 获取合并文件对象的路径
        String mergeFilePath = getFilePathByMd5(fileMd5, fileName.substring(fileName.lastIndexOf(".")));
        // 合并所有分块
        try {
            ObjectWriteResponse response = client.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(bucket_video)
                            .object(mergeFilePath)
                            .sources(sourceObjectList)
                            .build());
            log.debug("合并文件成功:{}",mergeFilePath);
        } catch (Exception e) {
            e.printStackTrace();
            log.debug("合并文件失败,fileMd5:{},异常:{}",fileMd5,e.getMessage(),e);
            return RestResponse.validfail(false, "合并文件失败。");
        }

        // 校验分块正确性
        // 从minio中下载合并后的文件并获取相应的md5值
        File minioFile = downloadFileFromMinIO(bucket_video, mergeFilePath);
        String newMd5 = getMd5(minioFile);
        // 检验与文件原始的md5值是否相同
        if(!fileMd5.equals(newMd5)){
            log.error("文件校验失败，原始文件:{}, 合并文件:{}", fileMd5, newMd5);
            return RestResponse.success(false, "文件校验失败");
        }
        // 存储合并文件到数据库
        MediaFiles file = currentProxy.updateMediaDB(companyId, mergeFilePath, uploadFileParams, fileMd5, bucket_video);
        if(file == null) {
            return RestResponse.validfail("false", "文件入库失败");
        }
        // 清理分块文件
        // clearChunkFiles(mergeFilePath, chunkTotal);
        return RestResponse.success(true);
    }

    /**
     * 清除分块文件
     * @param path 删除路径
     * @param chunkTotal 删除块数
     */
    private void clearChunkFiles(String path, int chunkTotal) {
        try {
            List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                    .limit(chunkTotal)
                    .map(i -> new DeleteObject(path.concat(Integer.toString(i))))
                    .collect(Collectors.toList());

            RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket("video").objects(deleteObjects).build();
            Iterable<Result<DeleteError>> results = client.removeObjects(removeObjectsArgs);
            results.forEach(r->{
                DeleteError deleteError = null;
                try {
                    deleteError = r.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("清楚分块文件失败,objectname:{}",deleteError.objectName(),e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("清楚分块文件失败,chunkFileFolderPath:{}",path,e);
        }
    }

    public File downloadFileFromMinIO(String bucket, String objectName) {
        File minioFile = null;
        FileOutputStream outputStream = null;
        try{
            InputStream stream = client.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            //创建临时文件
            minioFile=File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream,outputStream);
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(outputStream!=null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private String getFilePathByMd5(String fileMd5, String extension) {
        return fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" +fileMd5 +extension;
    }

    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
    }

    @Transactional
    public MediaFiles updateMediaDB(Long companyId, String name, UploadFileParams params, String md5, String bucket) {
        MediaFiles records = mediaFilesMapper.selectById(md5);
        if (records != null) return records;

        records = new MediaFiles();
        records.setCompanyId(companyId);
        records.setFilename(params.getFilename());
        records.setFileType(params.getFileType());
        records.setBucket(bucket);
        records.setFilePath(name);
        records.setFileId(md5);
        records.setUrl("/" + bucket + "/" + name);
        records.setCreateDate(LocalDateTime.now());
        records.setStatus("1");
        records.setAuditStatus("002003");
        records.setFileSize(params.getFileSize());

        if (mediaFilesMapper.insert(records) < 0) {
            throw new ULearnException("保存文件信息失败");
        }

        // 添加文件信息到待处理文件表
        addWaitingTask(records);
        log.debug("保存文件信息到数据库成功,{}", records.toString());
        return records;
    }

    /**
     * 上传文件至MINIO
     * @param path 文件名——源文件地址
     * @param name 设置该文件在MINIO中的对象名
     * @param mimeType mimeType
     * @param bucket 设置存入MINIO中的桶名
     * @return 是否存入成功
     */
    public boolean uploadFileToMinio(String path, String name, String mimeType, String bucket) {
        try {
            UploadObjectArgs args = UploadObjectArgs.builder().
                    filename(path).
                    object(name).
                    contentType(mimeType).
                    bucket(bucket)
                    .build();

            client.uploadObject(args);
            return true;
        } catch (Exception e) {
            throw new ULearnException("上传文件失败");
        }
    }

    @Override
    public MediaFiles getFileById(String mediaId) {
        return mediaFilesMapper.selectById(mediaId);
    }

    private String getMd5(File file) {
        try (FileInputStream stream = new FileInputStream(file)) {
            return DigestUtils.md5DigestAsHex(stream);
        } catch (Exception e) {
            return null;
        }
    }

    private String genMinioPath() {
        SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd");
        return formater.format(new Date()).replace('-', '/');
    }

    private String getMIMEType(String extension) {
        if (extension == null) extension = "";
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }

    private void addWaitingTask(MediaFiles mediaFiles) {
        //文件名称
        String filename = mediaFiles.getFilename();
        //文件扩展名
        String exension = filename.substring(filename.lastIndexOf("."));
        //文件mimeType
        String mimeType = getMIMEType(exension);
        //如果是avi视频添加到视频待处理表
        if(mimeType.equals("video/x-msvideo")) {
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles, mediaProcess);
            mediaProcess.setStatus("1");//未处理
            mediaProcess.setFailCount(0);//失败次数默认为0
            mediaProcessMapper.insert(mediaProcess);
        }
    }
}
