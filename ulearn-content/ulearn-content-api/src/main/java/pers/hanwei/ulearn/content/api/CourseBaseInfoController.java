package pers.hanwei.ulearn.content.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pers.hanwei.ulearn.base.exception.ValidationGroups;
import pers.hanwei.ulearn.base.model.PageParams;
import pers.hanwei.ulearn.base.model.PageResult;
import pers.hanwei.ulearn.content.model.dto.AddCourseDto;
import pers.hanwei.ulearn.content.model.dto.CourseBaseInfoDto;
import pers.hanwei.ulearn.content.model.dto.EditCourseDto;
import pers.hanwei.ulearn.content.model.dto.QueryCourseParamsDto;
import pers.hanwei.ulearn.content.model.po.CourseBase;
import pers.hanwei.ulearn.content.service.ICourseBaseInfoService;
import pers.hanwei.ulearn.content.service.impl.CourseBaseInfoService;

import javax.validation.Valid;

@Api(value = "课程信息编辑接口", tags = "课程信息编辑接口")
@RestController
public class CourseBaseInfoController {
    @Autowired
    ICourseBaseInfoService courseBaseInfoService;

    @ApiOperation(value = "课程查询接口")
    @PostMapping("/course/list")
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody QueryCourseParamsDto queryCourseParams) {
        return courseBaseInfoService.queryCourseBaseList(pageParams, queryCourseParams);
    }

    @ApiOperation(value = "新增课程接口")
    @PostMapping("/course")
    public CourseBaseInfoDto addBaseCourseInfo(@RequestBody @Validated({ValidationGroups.Insert.class}) AddCourseDto addCourseDto) {
        // 由于暂时没有认证系统 这里暂时设置为硬编码！！！
        return courseBaseInfoService.addCourseBaseInfo(1232141425L, addCourseDto);
    }

    @GetMapping(value = "/course/{courseId}")
    @ApiOperation(value = "根据课程id查询课程基本信息接口")
    public CourseBaseInfoDto getCourseBaseInfoById(@PathVariable Long courseId) {
        return courseBaseInfoService.getCourseBaseInfo(courseId);
    }

    @PutMapping(value = "/course")
    @ApiOperation(value = "修改课程接口")
    public CourseBaseInfoDto editBaseCourseInfo(Long companyId, @RequestBody @Validated EditCourseDto editCourseDto) {
        return courseBaseInfoService.editCourseBaseInfo(companyId, editCourseDto);
    }
}