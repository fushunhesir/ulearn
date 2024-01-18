package pers.hanwei.ulearn.content.service;

import pers.hanwei.ulearn.content.model.dto.AddTeacherDto;
import pers.hanwei.ulearn.content.model.po.CourseTeacher;

import java.util.List;

public interface ITeacherService {
    List<CourseTeacher> queryTeacher(Long courseId);

    CourseTeacher editTeacher(CourseTeacher teacher);

    CourseTeacher updateTeacher(CourseTeacher teacher);

    void deleteTeacher(Long courseId, Long teacherId);
}
