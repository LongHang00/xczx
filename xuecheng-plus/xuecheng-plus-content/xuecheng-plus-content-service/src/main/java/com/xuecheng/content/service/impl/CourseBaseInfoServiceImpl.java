package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    private final CourseBaseMapper courseBaseMapper;

    /**
     * 查询课程
     * @param pageParams
     * @param qDto
     * @return
     */
    @Override
    public PageResult<CourseBase> queryCourseBase(PageParams pageParams, QueryCourseParamsDto qDto) {
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //查询条件
//        QueryCourseParamsDto qDto = new QueryCourseParamsDto();
//        qDto.setCourseName("java");
//        qDto.setAuditStatus("202004");
//        qDto.setPublishStatus("203001");
        queryWrapper.like(StringUtils.isNotEmpty(qDto.getCourseName()),CourseBase::getName,qDto.getCourseName());
        queryWrapper.eq(StringUtils.isNotEmpty(qDto.getAuditStatus()),CourseBase::getAuditStatus,qDto.getAuditStatus());
        queryWrapper.eq(StringUtils.isNotEmpty(qDto.getPublishStatus()),CourseBase::getStatus,qDto.getPublishStatus());
//        PageParams params = new PageParams();
//        params.setPageNo(1l);
//        params.setPageSize(10l);
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(),pageParams.getPageSize());
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        PageResult<CourseBase> result = new PageResult<>(pageResult.getRecords(),pageResult.getTotal(),pageParams.getPageNo(),pageParams.getPageSize());
        System.out.println(result);
        return result;
    }
}
