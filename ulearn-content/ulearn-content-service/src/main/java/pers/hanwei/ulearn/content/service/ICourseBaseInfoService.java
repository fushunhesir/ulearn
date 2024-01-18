package pers.hanwei.ulearn.content.service;

import pers.hanwei.ulearn.base.model.PageParams;
import pers.hanwei.ulearn.base.model.PageResult;
import pers.hanwei.ulearn.content.model.dto.AddCourseDto;
import pers.hanwei.ulearn.content.model.dto.CourseBaseInfoDto;
import pers.hanwei.ulearn.content.model.dto.EditCourseDto;
import pers.hanwei.ulearn.content.model.dto.QueryCourseParamsDto;
import pers.hanwei.ulearn.content.model.po.CourseBase;

public interface ICourseBaseInfoService {
    /**
     * 课程分页查询
     * @param pageParams 分页查询参数
     * @param courseParamsDto 查询条件
     * @return 查询结果
     */
    PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto courseParamsDto);


    /**
     * 根据课程id查询课程信息
     * @param courseId 课程id
     * @return 课程详细信息
     */
    CourseBaseInfoDto getCourseBaseInfo(Long courseId);

    CourseBaseInfoDto addCourseBaseInfo(Long companyId, AddCourseDto addCourseDto);

    CourseBaseInfoDto editCourseBaseInfo(Long companyId, EditCourseDto editCourseDto);
}
