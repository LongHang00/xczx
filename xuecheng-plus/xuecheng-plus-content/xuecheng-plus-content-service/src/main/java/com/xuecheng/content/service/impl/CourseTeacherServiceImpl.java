package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.dto.CourseTeacherDto;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseTeacherServiceImpl implements CourseTeacherService {

    private final CourseTeacherMapper courseTeacherMapper;

    /**
     * 查询老师信息
     * @param courseId
     * @return
     */
    @Override
    public List<CourseTeacher> listCourseTeacher(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseId);
        List<CourseTeacher> courseTeachers = courseTeacherMapper.selectList(queryWrapper);
//        List<CourseTeacher> courseTeacherDto = new ArrayList<>();
//        BeanUtils.copyProperties(courseTeachers,courseTeacherDto);
//        courseTeachers.stream().forEach(item->courseTeacherDto.add(item));
        return courseTeachers;
    }

    /**
     * 添加/修改老师信息
     * @param courseTeacherDto
     * @return
     */
    @Transactional
    @Override
    public CourseTeacher addCourseTeacher(CourseTeacherDto courseTeacherDto) {
        if (courseTeacherDto.getId()==null){
            //添加老师信息
            courseTeacherDto.setCreateDate(LocalDateTime.now());
            courseTeacherMapper.insert(courseTeacherDto);
//        LambdaQueryWrapper<CourseTeacherDto> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(CourseTeacherDto::getCourseId,courseTeacherDto.getCourseId());
            CourseTeacher courseTeacher = courseTeacherMapper.selectById(courseTeacherDto.getId());
            return courseTeacher;
        }
        //修改老师信息
        courseTeacherMapper.updateById(courseTeacherDto);
        CourseTeacher courseTeacher = courseTeacherMapper.selectById(courseTeacherDto.getId());
        return courseTeacher;
    }

//    /**
//     * 修改老师信息
//     * @param courseTeacherDto
//     * @return
//     */
//    @Transactional
//    @Override
//    public CourseTeacher updateCourseTeacher(CourseTeacherDto courseTeacherDto) {
//        courseTeacherMapper.updateById(courseTeacherDto);
//        CourseTeacher courseTeacher = courseTeacherMapper.selectById(courseTeacherDto.getId());
//        return courseTeacher;
//    }

    /**
     * 删除老师信息
     * @param courseId
     * @param courseTeacherId
     */
    @Transactional
    @Override
    public void deleteCourseTeacher(Long courseId, Long courseTeacherId) {
        courseTeacherMapper.deleteById(courseTeacherId);
    }
}
