package pers.hanwei.ulearn.content.api;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import pers.hanwei.ulearn.content.model.dto.CoursePreviewDto;
import pers.hanwei.ulearn.content.service.CoursePublishService;

/**
 * 课程发布服务
 */
@Controller
public class CoursePublishController {
    @Autowired
    CoursePublishService coursePublishService;


    /**
     * 将课程数据填入课程预览模版
     * @param courseId 课程id
     */
    @GetMapping("/coursepreview/{courseId}")
    public ModelAndView coursePreview(@PathVariable Long courseId) {
        // 提供填入模版的数据
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(courseId);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("model",coursePreviewInfo);
        modelAndView.setViewName("course_template");
        return modelAndView;
    }



    @ApiOperation("提交审核接口")
    @PostMapping("/courseaudit/commit/{courseId}")
    public void commitAudit(@PathVariable("courseId") Long courseId) {
        Long companyId = 1232141425L;
        coursePublishService.commitAudit(companyId, courseId);
    }


    @ApiOperation("课程发布接口")
    @PostMapping("/coursepublish/{courseId}")
    public void publishCourse(@PathVariable("courseId") Long courseId) {
        Long companyId = 1232141425L;
        coursePublishService.publishCourse(companyId, courseId);
    }
}
