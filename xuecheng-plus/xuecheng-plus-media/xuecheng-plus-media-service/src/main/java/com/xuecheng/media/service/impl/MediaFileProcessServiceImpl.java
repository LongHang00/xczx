package com.xuecheng.media.service.impl;

import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaFileProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
@Service
@Slf4j
@RequiredArgsConstructor
public class MediaFileProcessServiceImpl implements MediaFileProcessService {

    private final MediaProcessMapper mediaProcessMapper;

    private final MediaFilesMapper mediaFilesMapper;

    private final MediaProcessHistoryMapper mediaProcessHistoryMapper;

    /**
     * 获取待处理任务
     * @param shardIndex
     * @param shardTotal
     * @param count
     * @return
     */
    @Override
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count) {
        List<MediaProcess> mediaProcesses = mediaProcessMapper.selectListByShardIndex(shardTotal, shardIndex, count);
        return mediaProcesses;
    }

    /**
     * 开启一个任务
     * @param id 任务id
     * @return 更新记录数
     */
    @Override
    public Boolean startTask(Long id) {
        int result = mediaProcessMapper.startTask(id);
        return result<=0?false:true;
    }


    /**
     * @description 保存任务结果
     * @param taskId  任务id
     * @param status 任务状态
     * @param fileId  文件id
     * @param url url
     * @param errorMsg 错误信息
     * @return void
     * @author Mr.M
     * @date 2022/10/15 11:29
     */
    @Override
    @Transactional
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {
        //查出任务，如果不存在直接返回
        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if (mediaProcess==null){
            return;
        }
        //处理失败，更新任务处理结果
        if (status.equals("3")){
            mediaProcess.setStatus(status);
            mediaProcess.setFailCount(mediaProcess.getFailCount()+1);
            mediaProcess.setErrormsg(errorMsg);
            mediaProcessMapper.updateById(mediaProcess);
            log.debug("更新任务处理状态为失败，任务信息:{}",errorMsg);
            return ;
        }
        //任务处理成功
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        if (mediaFiles!=null){
            mediaFiles.setUrl(url);
            mediaFilesMapper.updateById(mediaFiles);
        }
        //处理成功，更新url和状态
        mediaProcess.setStatus("2");
        mediaProcess.setUrl(url);
        mediaProcess.setFinishDate(LocalDateTime.now());
        mediaProcessMapper.updateById(mediaProcess);
        //添加到历史记录
        MediaProcessHistory mediaProcessHistory  = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess,mediaProcessHistory);
        mediaProcessHistoryMapper.insert(mediaProcessHistory);
        //删除mediaProcess
        mediaProcessMapper.deleteById(mediaProcess.getId());
    }
}
