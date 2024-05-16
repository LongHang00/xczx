package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

public interface TeachplanService {


    /**
     * 查询课程计划树型结构
     * @param courseId
     * @return
     */
    List<TeachplanDto> findTeachplanTree(Long courseId);


    /**
     * 新增计划和修改计划
     * @param teachplanDto
     */
    void sevaTeachplan(TeachplanDto teachplanDto);
}
