package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TeachplanServiceImpl implements TeachplanService {


    private final TeachplanMapper teachplanMapper;

    private final TeachplanMediaMapper teachplanMediaMapper;

    /**
     * 查询课程计划树型结构
     * @param courseId
     * @return
     */
    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(courseId);

        return teachplanDtos;
    }

    /**
     * 新增计划和修改计划
     * @param teachplanDto
     */
    @Transactional
    @Override
    public void sevaTeachplan(TeachplanDto teachplanDto) {
        //课程计划id，当有id时为修改课程，没有id时为添加课程
        Long id = teachplanDto.getId();
        if (id == null){
            //新增课程计划
            int count = getTeachplanCount(teachplanDto.getCourseId(), teachplanDto.getParentid());
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(teachplanDto,teachplan);
            teachplan.setOrderby(count+1);
            teachplan.setCreateDate(LocalDateTime.now());
            teachplanMapper.insert(teachplan);
        }else {
            //修改课程计划
            Teachplan teachplan = teachplanMapper.selectById(id);
            BeanUtils.copyProperties(teachplanDto,teachplan);
            teachplan.setChangeDate(LocalDateTime.now());
            teachplanMapper.updateById(teachplan);
        }
    }
    //获得最新排序
    private int getTeachplanCount(long courseId,long parentId){
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,courseId);
        queryWrapper.eq(Teachplan::getParentid,parentId);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return count;
    }


    /**
     * 课程计划删除
     * @param id
     */
    @Override
    @Transactional
    public void deleteTeachplan(Long id) {
        Teachplan teachplan = teachplanMapper.selectById(id);
        //删除第一级别的大章节时要求大章节下边没有小章节时方可删除。
        if (teachplan.getGrade()==1){
            List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNode(id);
            if (teachplanDtos.size()!=0){
                throw new XueChengPlusException("课程计划信息还有子级信息，无法操作");
            }else {
                teachplanMapper.deleteById(id);
            }
        }else if(teachplan.getGrade()==2){
            //删除第二级别的小章节的同时需要将teachplan_media表关联的信息也删除。
            teachplanMapper.deleteById(id);
            LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TeachplanMedia::getTeachplanId,id);
            teachplanMediaMapper.delete(queryWrapper);
        }



    }

    /**
     * 课程计划排序（向上移动）
     * @param id
     */
    @Transactional
    @Override
    public void sortMoveupTeachplan(Long id) {
        Teachplan teachplan = teachplanMapper.selectById(id);
        teachplan.setOrderby(teachplan.getOrderby()-1);
        teachplanMapper.updateById(teachplan);
    }

    /**
     * 课程计划排序（向下移动）
     * @param id
     */
    @Transactional
    @Override
    public void sortMovedownTeachplan(Long id) {
        Teachplan teachplan = teachplanMapper.selectById(id);
        teachplan.setOrderby(teachplan.getOrderby()+1);
        teachplanMapper.updateById(teachplan);
    }


}
