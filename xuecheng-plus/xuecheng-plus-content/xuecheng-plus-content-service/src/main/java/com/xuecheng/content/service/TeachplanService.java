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

    /**
     * 课程计划删除
     * @param id
     */
    void deleteTeachplan(Long id);

    /**
     * 课程计划排序（向上移动）
     * @param id
     */
    void sortMoveupTeachplan(Long id);

    /**
     * 课程计划排序（向下移动）
     * @param id
     */
    void sortMovedownTeachplan(Long id);
}
