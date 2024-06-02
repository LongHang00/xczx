package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class CoursePublishServiceImpl implements CoursePublishService {

    private final CourseBaseInfoService courseBaseInfoService;

    private final TeachplanService teachplanService;

    private final CourseBaseMapper courseBaseMapper;

    private final CourseMarketMapper courseMarketMapper;

    private final CoursePublishPreMapper coursePublishPreMapper;

    private final CoursePublishMapper coursePublishMapper;

    private final MqMessageService mqMessageService;

    /**
     * @description 课程预览、发布接口
     * @version 1.0
     */
    @Override
    public CoursePreviewDto selectId(Long courseId) {
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.selectById(courseId);
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        coursePreviewDto.setCourseBase(courseBaseInfoDto);
        coursePreviewDto.setTeachplans(teachplanTree);
        return coursePreviewDto;
    }

    /**
     * 提交审核
     * @param companyId
     * @param courseId
     */
    @Override
    public void commitAudit(Long companyId, Long courseId) {
        //检验课程是否已经提交
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        String auditStatus = courseBase.getAuditStatus();
        if ("202003".equals(auditStatus)){
            //已经提交不用在提交
            XueChengPlusException.cast("当前是已经提交，等审核完成之后可以再提交");
        }
        //本机构只允许提交本机构的课程
        if(!courseBase.getCompanyId().equals(companyId)){
            XueChengPlusException.cast("不允许提交其它机构的课程。");
        }
        //添加课程预发布记录
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        //课程基本信息加部分营销信息
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.selectById(courseId);
        BeanUtils.copyProperties(courseBaseInfoDto,coursePublishPre);
        //添加课程营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        String courseMarketJson = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(courseMarketJson);
        //添加课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        if (teachplanTree.size()<=0){
            XueChengPlusException.cast("提交失败，还没有添加课程计划");
        }
        String teachplanTreeJson = JSON.toJSONString(teachplanTree);
        coursePublishPre.setTeachers(teachplanTreeJson);
        coursePublishPre.setCompanyId(companyId);
        coursePublishPre.setCreateDate(LocalDateTime.now());
        //判断课程预发布是否有记录，如果有为更新，如果没有为添加
        CoursePublishPre coursePublishPre1 = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre1==null){
            //新增记录
            coursePublishPreMapper.insert(coursePublishPre);
        }else {
            //更新记录
            coursePublishPreMapper.updateById(coursePublishPre);
        }

        //更新课程基本表的审核状态
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);
    }

    /**
     * 课程发布
     * @param companyId
     * @param courseId
     */
    @Override
    public void publish(Long companyId, Long courseId) {
        //检验课程
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPre == null){
            XueChengPlusException.cast("请先提交课程审核，审核通过才可以发布");
        }
        //本机构只允许提交本机构的课程
        if(!coursePublishPre.getCompanyId().equals(companyId)){
            XueChengPlusException.cast("不允许提交其它机构的课程。");
        }

        //课程审核状态
        String auditStatus = coursePublishPre.getStatus();
        //审核通过方可发布
        if(!"202004".equals(auditStatus)){
            XueChengPlusException.cast("操作失败，课程审核通过方可发布。");
        }

        //保存课程发布信息
        saveCoursePublish(courseId);

        //保存消息表
        saveCoursePublishMessage(courseId);

        //删除课程预发布表对应记录
        coursePublishPreMapper.deleteById(courseId);



    }

    //保存消息表
    private void saveCoursePublishMessage(Long courseId) {
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if(mqMessage==null){
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }

    }

    //保存课程发布信息
    private void saveCoursePublish(Long courseId) {
        //检验预发课程
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPre==null){
            XueChengPlusException.cast("课程预发数据为空");
        }
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre,coursePublish);
        coursePublish.setStatus("203002");
        CoursePublish coursePublish1 = coursePublishMapper.selectById(courseId);
        //判断是否为空，为空为新增，有值为更新
        if (coursePublish1==null){
            //新增
            coursePublishMapper.insert(coursePublish);
        }else {
            //更新
            coursePublishMapper.updateById(coursePublish);
        }

        //更新课程基本表的发布状态
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setStatus("203002");
        courseBaseMapper.updateById(courseBase);

    }
}
