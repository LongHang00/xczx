package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CourseTeacherDto;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@Api(value = "教师信息编辑接口",tags = "教师信息编辑接口")
public class CourseTeacherController {

    private final CourseTeacherService courseTeacherService;

    @ApiOperation("查询教师信息")
    @GetMapping("/courseTeacher/list/{courseId}")
    public List<CourseTeacher> selectCourseTeacher(@PathVariable Long courseId){
        List<CourseTeacher> courseTeacher= courseTeacherService.listCourseTeacher(courseId);
        return courseTeacher;
    }

    @ApiOperation("添加/修改老师信息")
    @PostMapping("/courseTeacher")
    public CourseTeacher addCourseTeacher(@RequestBody CourseTeacherDto courseTeacherDto){
        CourseTeacher courseTeacher = courseTeacherService.addCourseTeacher(courseTeacherDto);
        return courseTeacher;
    }

//    @ApiOperation("修改老师信息")
//    @PutMapping("/courseTeacher")
//    public CourseTeacher updateCourseTeacher(@RequestBody CourseTeacherDto courseTeacherDto){
//        CourseTeacher courseTeacher = courseTeacherService.updateCourseTeacher(courseTeacherDto);
//        return courseTeacher;
//    }

    @ApiOperation("删除老师信息")
    @DeleteMapping("/courseTeacher/course/{courseId}/{courseTeacherId}")
    public void deleteCourseTeacher(@PathVariable("courseId") Long courseId , @PathVariable("courseTeacherId") Long courseTeacherId){
        courseTeacherService.deleteCourseTeacher(courseId,courseTeacherId);
    }

}
