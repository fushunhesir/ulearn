package pers.hanwei.ulearn.content.service;

import pers.hanwei.ulearn.content.model.dto.CoursePreviewDto;

public interface CoursePublishService {

    CoursePreviewDto getCoursePreviewInfo(Long courseId);

    void commitAudit(Long companyId, Long courseId);

    void publishCourse(Long companyId, Long courseId);
}
