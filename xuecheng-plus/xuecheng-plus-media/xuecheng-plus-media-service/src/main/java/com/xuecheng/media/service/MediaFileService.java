package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.po.MediaFiles;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @version 1.0
 * @description 媒资文件管理业务类
 * @date 2022/9/10 8:55
 */
public interface MediaFileService {

    /**
     * 媒资文件查询方法
     *
     * @param pageParams          分页参数
     * @param queryMediaParamsDto 查询条件
     * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
     * @date 2022/9/10 8:57
     */
    PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

    /**
     * 上传图片文件
     *
     * @param companyId
     * @param uploadFileParamsDto
     * @param localFilePath
     * @return
     */
    UploadFileParamsDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath);


    /**
     * 检查上传文件
     * @param fileMd5
     * @return
     */
    RestResponse<Boolean> checkfile(String fileMd5);

    /**
     * 检查上传分块文件
     * @param fileMd5
     * @param chunk
     * @return
     */
    RestResponse<Boolean> checkchunk(String fileMd5,int chunk);

    /**
     * 上传分块文件
     * @param file
     * @param fileMd5
     * @param chunk
     * @return
     */
    RestResponse uploadchunk(MultipartFile file,String fileMd5,int chunk);

    /**
     * 合并上传文件
     * @param fileMd5
     * @param chunkTotal
     * @param uploadFileParamsDto
     * @return
     */
    RestResponse mergechunks(Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto,int chunkTotal);

    /**
     * 将数据保存到数据库中
     * @param companyId
     * @param fileMd5
     * @param uploadFileParamsDto
     * @param files
     * @param objectName
     * @return
     */
    MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String files, String objectName);
}
