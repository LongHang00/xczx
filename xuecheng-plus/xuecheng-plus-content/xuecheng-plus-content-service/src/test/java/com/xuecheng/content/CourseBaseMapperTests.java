package com.xuecheng.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;



@SpringBootTest
public class CourseBaseMapperTests {
    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Test
    void testCourseBaseMapper() {

        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //查询条件
        QueryCourseParamsDto qDto = new QueryCourseParamsDto();
        qDto.setCourseName("java");
        qDto.setAuditStatus("202004");
        qDto.setPublishStatus("203001");
        queryWrapper.like(StringUtils.isNotBlank(qDto.getCourseName()),CourseBase::getCompanyName,qDto.getCourseName());
        queryWrapper.eq(StringUtils.isNotBlank(qDto.getAuditStatus()),CourseBase::getAuditStatus,qDto.getAuditStatus());
        queryWrapper.eq(StringUtils.isNotBlank(qDto.getPublishStatus()),CourseBase::getStatus,qDto.getPublishStatus());
        PageParams params = new PageParams();
        params.setPageNo(1l);
        params.setPageSize(10l);
        Page<CourseBase> page = new Page<>(params.getPageNo(),params.getPageSize());
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        PageResult<CourseBase> result = new PageResult<>(pageResult.getRecords(),pageResult.getTotal(),params.getPageNo(),params.getPageSize());
        System.out.println(result);

    }
}
