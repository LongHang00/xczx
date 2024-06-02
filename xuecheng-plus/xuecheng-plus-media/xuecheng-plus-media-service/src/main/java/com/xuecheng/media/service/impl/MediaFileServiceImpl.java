package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.messages.DeleteObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @version 1.0
 * @description TODO
 * * @date 2022/9/10 8:58
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class MediaFileServiceImpl implements MediaFileService {


    private final MediaFilesMapper mediaFilesMapper;

    private final MinioClient minioClient;

    private final MediaProcessMapper mediaProcessMapper;

//    private final MediaFileService fileService;
    @Value("${minio.bucket.files}")
    private String files;
    @Value("${minio.bucket.videofiles}")
    private String video;

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

    /**
     * 检查文件是否存在
     *
     * @param fileMd5
     * @return
     */
    @Override
    public RestResponse<Boolean> checkfile(String fileMd5) {
        //检查数据库文件是否存在
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles != null) {
            String bucket = mediaFiles.getBucket();
            String filePath = mediaFiles.getFilePath();
            InputStream stream = null;
            try {
                stream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucket)
                                .object(filePath)
                                .build()
                );
                if (stream != null) {
                    //文件存在
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
            }


        }
        //文件不存在
        return RestResponse.success(false);
    }

    /**
     * 检查分块文件是否存在
     *
     * @param fileMd5
     * @param chunk
     * @return
     */
    @Override
    public RestResponse<Boolean> checkchunk(String fileMd5, int chunk) {
        //得到分块的目录
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //得到分块的路径
        String chunkFilePath = chunkFileFolderPath + chunk;
        try {
            InputStream stream = null;
            stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(video)
                            .object(chunkFilePath)
                            .build()
            );
            if (stream != null) {
                //分块已存在
                return RestResponse.success(true);
            }
        } catch (Exception e) {
        }
        //分块不存在
        return RestResponse.success(false);
    }

    /**
     * 上传分块文件
     *
     * @param file
     * @param fileMd5
     * @param chunk
     * @return
     */
    @Override
    public RestResponse uploadchunk(MultipartFile file, String fileMd5, int chunk) {
        String mimeType = getMimeType(null);
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //创建临时文件
        try {
            File tempFile = File.createTempFile("minio", "temp");
            //上传的文件拷贝到临时文件
            file.transferTo(tempFile);
            //文件路径
            String absolutePath = tempFile.getAbsolutePath();
            addMediaFilesToMinIO(absolutePath, mimeType, video, chunkFileFolderPath + chunk);
            return RestResponse.success(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RestResponse.validfail(false, "上传文件失败");
    }

    /**
     * 合并分块文件
     *
     * @param fileMd5
     * @param uploadFileParamsDto
     * @param chunkTotal
     * @return
     */
    @Override
    public RestResponse mergechunks(Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto, int chunkTotal) {
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //找到分块文件调用minio的接口进行合并
        List<ComposeSource> sources = Stream.iterate(0,i->++i).limit(chunkTotal).map(i->ComposeSource.builder().bucket(video).object(chunkFileFolderPath+i).build()).collect(Collectors.toList());
        //获取文件名
        String filename = uploadFileParamsDto.getFilename();
        //获取文件扩展名
        String substring = filename.substring(filename.lastIndexOf("."));
        //合并文件路径
        String filePathByMd5 = getFilePathByMd5(fileMd5, substring);
        try {
            minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .sources(sources)
                            .object(filePathByMd5)
                            .bucket(video)
                            .build()
            );
            log.debug("合并文件成功");
        }catch (Exception e){
            return RestResponse.validfail(false, "合并文件失败。");
        }
        //校验合并后和源文件是否一致
        //下载校验文件
        File file = downloadFileFromMinIO(video, filePathByMd5);
        try(FileInputStream fileInputStream = new FileInputStream(file)) {
            String s = DigestUtils.md5Hex(fileInputStream);
            if (!fileMd5.equals(s)){
                return RestResponse.validfail( false,"文件校验失败");
            }
            //文件大小
            uploadFileParamsDto.setFileSize(file.length());
        }catch (Exception e){
            return RestResponse.validfail( false,"文件校验失败");
        }
        //将文件信息入库
        MediaFiles mediaFiles = addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, video, filePathByMd5);
        if (mediaFiles==null){
            log.debug("文件入库失败");
        }
        //清理分块文件
        clearChunkFile(filePathByMd5,chunkTotal);
        return RestResponse.success(true);
    }

    /**
     * 上传图片文件
     *
     * @param companyId
     * @param uploadFileParamsDto
     * @param localFilePath
     * @return
     */
    @Override
    public UploadFileParamsDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {
        File file = new File(localFilePath);
        if (!file.exists()) {
            XueChengPlusException.cast("文件不存在");
        }
        //文件名称
        String filename = uploadFileParamsDto.getFilename();
        //文件扩展名
        String substring = filename.substring(filename.lastIndexOf("."));
        //文件mimeType
        String mimeType = getMimeType(substring);
        //文件的md5值
        String fileMd5 = getFileMd5(file);
        //文件的默认目录
        String defaultFolderPath = getDefaultFolderPath();
        //存储到minio中的对象名(带目录)
        String objectName = defaultFolderPath + fileMd5 + substring;
        //将文件上传到minio
        boolean bool = addMediaFilesToMinIO(localFilePath, mimeType, files, objectName);
        //设置文件大小
        uploadFileParamsDto.setFileSize(file.length());
        //将文件信息存储到数据库
        MediaFiles mediaFiles = addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, files, objectName);
        //准备返回数据
        BeanUtils.copyProperties(mediaFiles, uploadFileParamsDto);
        return uploadFileParamsDto;
    }

    //将数据保存到数据库中
    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        //文件没上传过
        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            mediaFiles.setId(fileMd5);
            mediaFiles.setCompanyId(companyId);
            mediaFiles.setFileId(fileMd5);
            mediaFiles.setBucket(bucket);
            mediaFiles.setFilePath(objectName);
            mediaFiles.setUrl("/" + bucket + "/" + objectName);
            mediaFiles.setAuditStatus("002003");
            mediaFiles.setCreateDate(LocalDateTime.now());
            mediaFiles.setStatus("1");
            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert < 0) {
                log.error("保存文件信息到数据库失败,{}", mediaFiles.toString());
                XueChengPlusException.cast("保存文件信息失败");
            }
            log.debug("保存文件信息到数据库成功,{}", mediaFiles.toString());

            addWaitingTask(mediaFiles);
        }
        return mediaFiles;
    }
    //添加待处理任务
    private void addWaitingTask(MediaFiles mediaFiles){
        //文件名称
        String filename = mediaFiles.getFilename();
        //文件扩展名
        String exension = filename.substring(filename.lastIndexOf("."));
        String mimeType = getMimeType(exension);
        if (mimeType.equals("video/x-msvideo")){
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles,mediaProcess);
            mediaProcess.setStatus("1");
            mediaProcess.setFailCount(0);
            mediaProcessMapper.insert(mediaProcess);
        }
    }
    //获取文件mimeType
    private String getMimeType(String extension) {
        if (extension == null) {
            extension = "";
        }
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }

    //获取文件默认存储目录路径 年/月/日
    private String getDefaultFolderPath() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String s = simpleDateFormat.format(new Date()).replace("-", "/") + "/";
        return s;
    }

    //获取文件的md5
    private String getFileMd5(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            String md5 = DigestUtils.md5Hex(fis);
            return md5;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


    }

    //上传文件
    public boolean addMediaFilesToMinIO(String localFilePath, String mimeType, String bucket, String objectName) {

        try {
            UploadObjectArgs upload = UploadObjectArgs.builder()
                    .contentType(mimeType)
                    .filename(localFilePath)
                    .bucket(bucket)
                    .object(objectName)
                    .build();

            minioClient.uploadObject(upload);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件到minio出错,bucket:{},objectName:{},错误原因:{}", bucket, objectName, e.getMessage(), e);
            XueChengPlusException.cast("上传文件到文件系统失败");
        }
        return false;
    }

    /**
     * 获得媒资信息
     * @param mediaId
     * @return
     */
    @Override
    public MediaFiles getFileById(String mediaId) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(mediaId);
        return mediaFiles;
    }

    //得到分块文件的目录
    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
    }
    //合并文件路径
    private String getFilePathByMd5(String fileMd5,String fileExt){
        return   fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" +fileMd5 +fileExt;
    }
    //下载文件
    public File downloadFileFromMinIO(String bucket,String objectName){
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try{
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
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
    //清理分块文件
    public void clearChunkFile(String filePathByMd5,int chunkTotal){
        Iterable<DeleteObject> objects = Stream.iterate(0,i->++i).limit(chunkTotal).map(i-> new DeleteObject(filePathByMd5+i)).collect(Collectors.toList());
        minioClient.removeObjects(RemoveObjectsArgs.builder().objects(objects).bucket(video).build());
    }


}
