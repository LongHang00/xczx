package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class CoursePublishController {

    private final CoursePublishService coursePublishService;

    /**
     * 课程预览接口
     *
     * @param courseId
     * @return
     */
    @GetMapping("/coursepreview/{courseId}")
    public ModelAndView preview(@PathVariable("courseId") Long courseId) {
        CoursePreviewDto coursePreviewDto = coursePublishService.selectId(courseId);
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("model", coursePreviewDto);
        modelAndView.setViewName("course_template");
        return modelAndView;
    }


    /**
     * 课程审核接口
     *
     * @param courseId
     */
    @ResponseBody
    @PostMapping("/courseaudit/commit/{courseId}")
    public void commitAudit(@PathVariable("courseId") Long courseId) {
        Long companyId = 1232141425L;
        coursePublishService.commitAudit(companyId,courseId);
    }

    @ApiOperation("课程发布")
    @ResponseBody
    @PostMapping ("/coursepublish/{courseId}")
    public void coursepublish(@PathVariable("courseId") Long courseId){

    }

}
