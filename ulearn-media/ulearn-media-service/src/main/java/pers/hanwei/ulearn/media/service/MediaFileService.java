package pers.hanwei.ulearn.media.service;

import pers.hanwei.ulearn.base.model.PageParams;
import pers.hanwei.ulearn.base.model.PageResult;
import pers.hanwei.ulearn.base.model.RestResponse;
import pers.hanwei.ulearn.media.model.UploadFileParams;
import pers.hanwei.ulearn.media.model.dto.QueryMediaParamsDto;
import pers.hanwei.ulearn.media.model.dto.UploadFileResultDto;
import pers.hanwei.ulearn.media.model.po.MediaFiles;

import java.io.File;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件管理业务类
 * @date 2022/9/10 8:55
 */
public interface MediaFileService {

    /**
     * @param pageParams          分页参数
     * @param queryMediaParamsDto 查询条件
     * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
     * @description 媒资文件查询方法
     * @author Mr.M
     * @date 2022/9/10 8:57
     */
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);


    UploadFileResultDto uploadFile(Long companyId, UploadFileParams params, String absolutePath);

    public RestResponse<Boolean> checkFile(String fileMd5);

    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex);

    public RestResponse uploadChunk(String fileMd5,int chunk,String localFilePath);

    public MediaFiles updateMediaDB(Long companyId, String name, UploadFileParams params, String md5, String bucket);

    public  RestResponse mergeChunk(Long companyId, String fileMd5, int chunkTotal, UploadFileParams uploadFileParams);

    public File downloadFileFromMinIO(String bucket, String objectName);

    public boolean uploadFileToMinio(String path, String name, String mimeType, String bucket);

    public MediaFiles getFileById(String mediaId);
}
