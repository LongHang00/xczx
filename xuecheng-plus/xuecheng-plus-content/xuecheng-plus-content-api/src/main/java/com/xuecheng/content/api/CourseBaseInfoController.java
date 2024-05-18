package com.xuecheng.content.api;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    @ApiOperation("新增课程")
    @PostMapping("/course")
    public CourseBaseInfoDto addCourse(@RequestBody(required = false) @Validated AddCourseDto addCourseDto){
        Long companyId = 1232141425L;
        CourseBaseInfoDto courseBaseInfoDto1 = courseBaseInfoService.addCourse(companyId, addCourseDto);
        return courseBaseInfoDto1;
    }

    @ApiOperation("根据课程id查询课程基础信息")
    @GetMapping("/course/{courseId}")
    public CourseBaseInfoDto getCourseBaseById(@PathVariable Long courseId){
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.selectById(courseId);
        return courseBaseInfoDto;
    }

    @ApiOperation("修改课程基础信息")
    @PutMapping("/course")
    public CourseBaseInfoDto modifyCourseBase(@RequestBody(required = false) @Validated EditCourseDto editCourseDto){
        Long companyId = 1232141425L;
        CourseBaseInfoDto courseBaseInfoDto =courseBaseInfoService.updateCourseBase(companyId,editCourseDto);
        return courseBaseInfoDto;
    }

    @ApiOperation("删除课程")
    @DeleteMapping("/course/{id}")
    public void deleteCourse(@PathVariable Long id){
        courseBaseInfoService.deleteCourse(id);
    }

}
