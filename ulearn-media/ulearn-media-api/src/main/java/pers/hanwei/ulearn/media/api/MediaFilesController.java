package pers.hanwei.ulearn.media.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pers.hanwei.ulearn.base.model.PageParams;
import pers.hanwei.ulearn.base.model.PageResult;
import pers.hanwei.ulearn.media.model.UploadFileParams;
import pers.hanwei.ulearn.media.model.dto.QueryMediaParamsDto;
import pers.hanwei.ulearn.media.model.dto.UploadFileResultDto;
import pers.hanwei.ulearn.media.model.po.MediaFiles;
import pers.hanwei.ulearn.media.service.MediaFileService;

import java.io.File;
import java.io.IOException;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件管理接口
 * @date 2022/9/6 11:29
 */
@Api(value = "媒资文件管理接口", tags = "媒资文件管理接口")
@RestController
public class MediaFilesController {


    @Autowired
    MediaFileService mediaFileService;


    @ApiOperation("媒资列表查询接口")
    @PostMapping("/files")
    public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto) {
        Long companyId = 1232141425L;
        return mediaFileService.queryMediaFiels(companyId, pageParams, queryMediaParamsDto);
    }

    @ApiOperation("文件上传接口")
    @RequestMapping(value = "/upload/coursefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadFileResultDto upload(@RequestPart("filedata") MultipartFile upload,
                                      @RequestParam(value = "folder",required=false) String folder,
                                      @RequestParam(value = "objectName",required=false) String objectName) throws IOException {
        /**
         * 上面的两个参数属于可选字段，当请求的参数里面有这两个字段时就有用否则就没有用
         */
        // 设置参数
        UploadFileParams params = new UploadFileParams();
        Long companyId = 1232141425L;
        params.setFilename(upload.getOriginalFilename());
        params.setFileType("001001");
        params.setFileSize(upload.getSize());

        // 将数据存储到临时文件
        File temp = File.createTempFile("minio", "temp");
        upload.transferTo(temp);

        // 将临时文件传输到minio
        return mediaFileService.uploadFile(companyId, params, temp.getAbsolutePath());
    }
}
