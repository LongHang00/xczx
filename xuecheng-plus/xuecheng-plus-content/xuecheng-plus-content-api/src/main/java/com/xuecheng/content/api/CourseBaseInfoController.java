package com.xuecheng.content.api;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "课程信息编辑接口",tags = "课程信息编辑接口")
@RestController
@RequiredArgsConstructor
public class CourseBaseInfoController {

    private final CourseBaseInfoService courseBaseInfoService;
    @ApiOperation("课程查询接口")
    @PostMapping("/course/list")
    public PageResult<CourseBase> list(PageParams pageParams,@RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto){
        PageResult<CourseBase> result = courseBaseInfoService.queryCourseBase(pageParams, queryCourseParamsDto);
        return result;
    }
}
