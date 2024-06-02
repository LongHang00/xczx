package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;

/**
 * @version 1.0
 * @description 课程预览、发布接口
 */

public interface CoursePublishService {

    CoursePreviewDto selectId(Long courseId);

    /**
     * 提交审核
     *
     * @param companyId
     * @param courseId
     */
    void commitAudit(Long companyId, Long courseId);

    /**
     * 课程发布
     * @param companyId
     * @param courseId
     */
    void publish(Long companyId, Long courseId);
}
