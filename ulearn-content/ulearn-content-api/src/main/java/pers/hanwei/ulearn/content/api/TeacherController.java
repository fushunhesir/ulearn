package pers.hanwei.ulearn.content.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pers.hanwei.ulearn.content.model.dto.AddTeacherDto;
import pers.hanwei.ulearn.content.model.po.CourseTeacher;
import pers.hanwei.ulearn.content.service.impl.TeacherService;

import java.util.List;

@ApiModel("教师管理模块")
@RestController
public class TeacherController {
    @Autowired
    TeacherService teacherService;

    @ApiOperation("教师添加/修改接口")
    @PostMapping("/courseTeacher")
    public CourseTeacher editTeacher(@RequestBody CourseTeacher teacher) {
        return teacherService.editTeacher(teacher);
    }

    @ApiOperation("教师删除接口")
    @DeleteMapping("/courseTeacher/course/{courseId}/{teacherId}")
    public void deleteTeacher(@PathVariable Long courseId, @PathVariable Long teacherId) {
        teacherService.deleteTeacher(courseId, teacherId);
    }

    @ApiOperation("教师查询接口")
    @GetMapping("/courseTeacher/list/{courseId}")
    public List<CourseTeacher> queryTeacher(@PathVariable Long courseId) {
        return teacherService.queryTeacher(courseId);
    }
}
