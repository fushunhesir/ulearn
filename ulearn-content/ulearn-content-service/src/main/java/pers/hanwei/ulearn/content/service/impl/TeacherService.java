package pers.hanwei.ulearn.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pers.hanwei.ulearn.content.mapper.CourseTeacherMapper;
import pers.hanwei.ulearn.content.model.po.CourseTeacher;
import pers.hanwei.ulearn.content.service.ITeacherService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TeacherService implements ITeacherService {
    @Autowired
    CourseTeacherMapper teacherMapper;

    /**
     * 教师查询服务
     * @param courseId 课程id
     * @return 教师列表
     */
    @Override
    public List<CourseTeacher> queryTeacher(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseTeacher::getCourseId, courseId);
        return teacherMapper.selectList(wrapper);
    }

    /**
     * 教师添加服务
     * @param teacher 教师参数
     * @return 教师参数
     */
    @Override
    public CourseTeacher editTeacher(CourseTeacher teacher) {
        if(teacher.getId() == null) {
            teacher.setCreateDate(LocalDateTime.now());
            teacherMapper.insert(teacher);
        } else {
            teacherMapper.updateById(teacher);
        }
        return teacher;
    }

    @Override
    public CourseTeacher updateTeacher(CourseTeacher teacher) {
        teacherMapper.updateById(teacher);
        return teacher;
    }

    @Override
    public void deleteTeacher(Long courseId, Long teacherId) {
        LambdaQueryWrapper<CourseTeacher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseTeacher::getId, teacherId);
        wrapper.eq(CourseTeacher::getCourseId, courseId);
        teacherMapper.delete(wrapper);
    }
}
