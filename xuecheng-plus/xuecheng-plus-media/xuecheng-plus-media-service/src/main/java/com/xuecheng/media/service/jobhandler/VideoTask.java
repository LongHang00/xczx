package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * XxlJob开发示例（Bean模式）
 * <p>
 * 开发步骤：
 * 1、任务开发：在Spring Bean实例中，开发Job方法；
 * 2、注解配置：为Job方法添加注解 "@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 * 3、执行日志：需要通过 "XxlJobHelper.log" 打印执行日志；
 * 4、任务结果：默认任务结果为 "成功" 状态，不需要主动设置；如有诉求，比如设置任务结果为失败，可以通过 "XxlJobHelper.handleFail/handleSuccess" 自主设置任务结果；
 *
 * @author xuxueli 2019-12-11 21:52:51
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class VideoTask {

    private final MediaFileProcessService mediaFileProcessService;

    private final MediaFileService mediaFileService;

    @Value("${videoprocess.ffmpegpath}")
    String ffmpegpath;

    /**
     * 1、简单任务示例（Bean模式）
     */
    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        List<MediaProcess> mediaProcessList = null;
        int size = 0;
        //取出cpu核心数作为一次处理数据的条数
        int processors = Runtime.getRuntime().availableProcessors();
        //一次处理视频数量不要超过cpu核心数,获取待处理的任务
        mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors);
        //启动size个线程的线程池
        size = mediaProcessList.size();
        log.debug("取出待处理视频任务{}条", size);
        if (size < 0) {
            return;
        }
        ExecutorService threadPool = Executors.newFixedThreadPool(size);
        //计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        //将处理任务加入线程池
        mediaProcessList.forEach(mediaProcess -> {
            threadPool.execute(() -> {
                try {
                    //任务id
                    Long taskId = mediaProcess.getId();
                    //抢占任务
                    Boolean b = mediaFileProcessService.startTask(taskId);
                    if (!b) {
                        return;
                    }
                    //桶
                    String bucket = mediaProcess.getBucket();
                    //存储路径
                    String filePath = mediaProcess.getFilePath();
                    //原始视频的md5值
                    String fileId = mediaProcess.getFileId();
                    //原始文件名称
                    String filename = mediaProcess.getFilename();
                    //将要处理的文件下载到服务器上
                    File file = mediaFileService.downloadFileFromMinIO(bucket, filePath);
                    if (file == null) {
                        log.debug("下载待处理文件失败,originalFile:{}", mediaProcess.getBucket().concat(mediaProcess.getFilePath()));
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "下载待处理文件失败");
                        return;
                    }
                    //处理结束的视频文件
                    File mp4File = null;
                    try {
                        mp4File = File.createTempFile("mp4", ".mp4");
                    } catch (Exception e) {
                        log.error("创建mp4临时文件失败");
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "创建mp4临时文件失败");
                        return;
                    }
                    String result = "";
                    try {
                        //处理视频
                        Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil(ffmpegpath, file.getAbsolutePath(), mp4File.getName(), mp4File.getAbsolutePath());
                        result = mp4VideoUtil.generateMp4();
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("处理视频文件:{},出错:{}", mediaProcess.getFilePath(), e.getMessage());
                        if (!result.equals("success")) {
                            //记录错误信息
                            log.error("处理视频失败,视频地址:{},错误信息:{}", bucket + filePath, result);
                            mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, result);
                            return;
                        }
                    }
                    //将文件传到minio上
                    String objectName = getFilePath(fileId, ".mp4");
                    String url = "/" + bucket + "/" + objectName;
                    try {
                        //将url存储至数据，并更新状态为成功，并将待处理视频记录删除存入历史
                        mediaFileService.addMediaFilesToMinIO(mp4File.getAbsolutePath(), "video/mp4", bucket, objectName);
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(),"2",fileId,url,null);
                    }catch (Exception e){
                        log.error("上传视频失败或入库失败,视频地址:{},错误信息:{}", bucket + objectName, e.getMessage());
                        //最终还是失败了
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "处理后视频上传或入库失败");
                    }
                }finally {
                    countDownLatch.countDown();
                }
            });
        });
        //等待,给一个充裕的超时时间,防止无限等待，到达超时时间还没有处理完成则结束任务
        countDownLatch.await(30, TimeUnit.MINUTES);
    }

    //得到上传文件路径
    private String getFilePath(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

//    /**
//     * 2、分片广播任务
//     */
//    @XxlJob("shardingJobHandler")
//    public void shardingJobHandler() throws Exception {
//
//        // 分片参数
//        int shardIndex = XxlJobHelper.getShardIndex();
//        int shardTotal = XxlJobHelper.getShardTotal();
//
//        XxlJobHelper.log("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);
//
//        // 业务逻辑
//        for (int i = 0; i < shardTotal; i++) {
//            if (i == shardIndex) {
//                XxlJobHelper.log("第 {} 片, 命中分片开始处理", i);
//            } else {
//                XxlJobHelper.log("第 {} 片, 忽略", i);
//            }
//        }
//
//    }
//
//
//    /**
//     * 3、命令行任务
//     */
//    @XxlJob("commandJobHandler")
//    public void commandJobHandler() throws Exception {
//        String command = XxlJobHelper.getJobParam();
//        int exitValue = -1;
//
//        BufferedReader bufferedReader = null;
//        try {
//            // command process
//            ProcessBuilder processBuilder = new ProcessBuilder();
//            processBuilder.command(command);
//            processBuilder.redirectErrorStream(true);
//
//            Process process = processBuilder.start();
//            //Process process = Runtime.getRuntime().exec(command);
//
//            BufferedInputStream bufferedInputStream = new BufferedInputStream(process.getInputStream());
//            bufferedReader = new BufferedReader(new InputStreamReader(bufferedInputStream));
//
//            // command log
//            String line;
//            while ((line = bufferedReader.readLine()) != null) {
//                XxlJobHelper.log(line);
//            }
//
//            // command exit
//            process.waitFor();
//            exitValue = process.exitValue();
//        } catch (Exception e) {
//            XxlJobHelper.log(e);
//        } finally {
//            if (bufferedReader != null) {
//                bufferedReader.close();
//            }
//        }
//
//        if (exitValue == 0) {
//            // default success
//        } else {
//            XxlJobHelper.handleFail("command exit value("+exitValue+") is failed");
//        }
//
//    }
//
//
//    /**
//     * 4、跨平台Http任务
//     *  参数示例：
//     *      "url: http://www.baidu.com\n" +
//     *      "method: get\n" +
//     *      "data: content\n";
//     */
//    @XxlJob("httpJobHandler")
//    public void httpJobHandler() throws Exception {
//
//        // param parse
//        String param = XxlJobHelper.getJobParam();
//        if (param==null || param.trim().length()==0) {
//            XxlJobHelper.log("param["+ param +"] invalid.");
//
//            XxlJobHelper.handleFail();
//            return;
//        }
//
//        String[] httpParams = param.split("\n");
//        String url = null;
//        String method = null;
//        String data = null;
//        for (String httpParam: httpParams) {
//            if (httpParam.startsWith("url:")) {
//                url = httpParam.substring(httpParam.indexOf("url:") + 4).trim();
//            }
//            if (httpParam.startsWith("method:")) {
//                method = httpParam.substring(httpParam.indexOf("method:") + 7).trim().toUpperCase();
//            }
//            if (httpParam.startsWith("data:")) {
//                data = httpParam.substring(httpParam.indexOf("data:") + 5).trim();
//            }
//        }
//
//        // param valid
//        if (url==null || url.trim().length()==0) {
//            XxlJobHelper.log("url["+ url +"] invalid.");
//
//            XxlJobHelper.handleFail();
//            return;
//        }
//        if (method==null || !Arrays.asList("GET", "POST").contains(method)) {
//            XxlJobHelper.log("method["+ method +"] invalid.");
//
//            XxlJobHelper.handleFail();
//            return;
//        }
//        boolean isPostMethod = method.equals("POST");
//
//        // request
//        HttpURLConnection connection = null;
//        BufferedReader bufferedReader = null;
//        try {
//            // connection
//            URL realUrl = new URL(url);
//            connection = (HttpURLConnection) realUrl.openConnection();
//
//            // connection setting
//            connection.setRequestMethod(method);
//            connection.setDoOutput(isPostMethod);
//            connection.setDoInput(true);
//            connection.setUseCaches(false);
//            connection.setReadTimeout(5 * 1000);
//            connection.setConnectTimeout(3 * 1000);
//            connection.setRequestProperty("connection", "Keep-Alive");
//            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
//            connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");
//
//            // do connection
//            connection.connect();
//
//            // data
//            if (isPostMethod && data!=null && data.trim().length()>0) {
//                DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
//                dataOutputStream.write(data.getBytes("UTF-8"));
//                dataOutputStream.flush();
//                dataOutputStream.close();
//            }
//
//            // valid StatusCode
//            int statusCode = connection.getResponseCode();
//            if (statusCode != 200) {
//                throw new RuntimeException("Http Request StatusCode(" + statusCode + ") Invalid.");
//            }
//
//            // result
//            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
//            StringBuilder result = new StringBuilder();
//            String line;
//            while ((line = bufferedReader.readLine()) != null) {
//                result.append(line);
//            }
//            String responseMsg = result.toString();
//
//            XxlJobHelper.log(responseMsg);
//
//            return;
//        } catch (Exception e) {
//            XxlJobHelper.log(e);
//
//            XxlJobHelper.handleFail();
//            return;
//        } finally {
//            try {
//                if (bufferedReader != null) {
//                    bufferedReader.close();
//                }
//                if (connection != null) {
//                    connection.disconnect();
//                }
//            } catch (Exception e2) {
//                XxlJobHelper.log(e2);
//            }
//        }
//
//    }
//
//    /**
//     * 5、生命周期任务示例：任务初始化与销毁时，支持自定义相关逻辑；
//     */
//    @XxlJob(value = "demoJobHandler2", init = "init", destroy = "destroy")
//    public void demoJobHandler2() throws Exception {
//        XxlJobHelper.log("XXL-JOB, Hello World.");
//    }
//    public void init(){
//        logger.info("init");
//    }
//    public void destroy(){
//        logger.info("destroy");
//    }
//

}
