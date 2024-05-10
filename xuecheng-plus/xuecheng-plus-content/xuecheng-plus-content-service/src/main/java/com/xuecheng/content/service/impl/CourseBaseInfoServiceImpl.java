package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    private final CourseBaseMapper courseBaseMapper;

    private final CourseMarketMapper courseMarketMapper;

    private final CourseCategoryMapper courseCategoryMapper;

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

    /**
     * 新增课程
     * @param addCourseDto
     */
    @Override
    public CourseBaseInfoDto addCourse(Long companyId, AddCourseDto addCourseDto) {

        //向课程基本信息表添加数据
        CourseBase courseBaseNew = new CourseBase();
        BeanUtils.copyProperties(addCourseDto,courseBaseNew);
        courseBaseNew.setCompanyId(companyId);
        courseBaseNew.setCreateDate(LocalDateTime.now());
        //审核状态默认值
        courseBaseNew.setAuditStatus("202002");
        courseBaseNew.setStatus("203001");
        int insert = courseBaseMapper.insert(courseBaseNew);
        if (insert<=0){
            throw new RuntimeException("添加课程失败");
        }
        //向课程营销添加数据
        CourseMarket courseMarketNew = new CourseMarket();
        BeanUtils.copyProperties(addCourseDto,courseMarketNew);
        courseMarketNew.setId(courseBaseNew.getId());
        savecourseMarket(courseMarketNew);

        //查询接口并返回
        CourseBase courseBase = courseBaseMapper.selectById(courseBaseNew.getId());
        if (courseBase==null){
            return null;
        }
        CourseMarket courseMarket = courseMarketMapper.selectById(courseMarketNew.getId());

        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);

        CourseCategory courseCategory = courseCategoryMapper.selectById(courseBase.getMt());
        courseBaseInfoDto.setMtName(courseCategory.getName());
        courseBaseInfoDto.setStName(courseCategory.getLabel());

        return courseBaseInfoDto;
    }
    //单独写一个营销方法，存在则更新，不存在则添加
    private int savecourseMarket(CourseMarket courseMarket){
        //参数合法性校验（如果课程收费，没有添加价格也要抛异常）
        if (courseMarket.getCharge().equals("201001")){
            if (courseMarket.getPrice()==null || courseMarket.getPrice().floatValue()<=0){
                throw new RuntimeException("课程的价格不能为空并且不能小于等于0");
            }
        }
        //从数据库查询，存在更新，不存在添加
        Long id= courseMarket.getId();
        CourseMarket courseMarket1 = courseMarketMapper.selectById(id);
        if (courseMarket1==null){
            int insert = courseMarketMapper.insert(courseMarket);
            return insert;
        }
        BeanUtils.copyProperties(courseMarket,courseMarket1);
        int insert = courseMarketMapper.updateById(courseMarket1);
        return insert;
    }
}
