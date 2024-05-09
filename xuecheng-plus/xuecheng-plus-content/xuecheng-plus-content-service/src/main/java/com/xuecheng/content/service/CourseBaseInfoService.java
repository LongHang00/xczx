package com.xuecheng.content.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;

public interface CourseBaseInfoService {

    /**
     * 查询课程
     * @param pageParams
     * @param queryCourseParamsDto
     * @return
     */
    public PageResult<CourseBase> queryCourseBase(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto);

}
