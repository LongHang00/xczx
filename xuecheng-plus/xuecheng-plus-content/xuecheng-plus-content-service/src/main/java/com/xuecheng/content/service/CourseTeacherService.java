package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CourseTeacherDto;
import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

public interface CourseTeacherService {

    /**
     * 查询老师信息
     * @param courseId
     * @return
     */
    List<CourseTeacher> listCourseTeacher(Long courseId);

    /**
     * 添加老师信息
     * @param courseTeacherDto
     * @return
     */
    CourseTeacher addCourseTeacher(CourseTeacherDto courseTeacherDto);

//    /**
//     * 修改老师信息
//     * @param courseTeacherDto
//     * @return
//     */
//    CourseTeacher updateCourseTeacher(CourseTeacherDto courseTeacherDto);

    /**
     * 删除老师信息
     * @param courseId
     * @param courseTeacherId
     */
    void deleteCourseTeacher(Long courseId, Long courseTeacherId);
}
