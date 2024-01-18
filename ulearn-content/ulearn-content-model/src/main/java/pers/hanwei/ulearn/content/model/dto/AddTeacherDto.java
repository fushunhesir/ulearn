package pers.hanwei.ulearn.content.model.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@ApiModel(value = "TeacherDto", description = "课程老师信息")
@Data
public class AddTeacherDto {

    /**
     * 课程id
     */
    private Long courseId;

    /**
     * 教师名称
     */
    private String teacherName;

    /**
     * 教师职位
     */
    private String position;

    /**
     * 教师简介
     */
    private String introduction;
}
