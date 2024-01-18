package pers.hanwei.ulearn.content.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pers.hanwei.ulearn.content.model.dto.SaveTeachplanDto;
import pers.hanwei.ulearn.content.model.dto.TeachplanDto;
import pers.hanwei.ulearn.content.service.impl.TeachPlanService;

import java.util.List;

@RestController
@ApiModel(value = "课程计划信息管理")
public class TeachPlanController {
    @Autowired
    TeachPlanService teachPlanService;

    /**
     * 查询课程计划信息
     * @param courseId 课程Id
     * @return 课程计划信息DTO
     */
    @GetMapping(value = "/teachplan/{courseId}/tree-nodes")
    @ApiOperation("课程计划信息查询接口")
    public List<TeachplanDto> queryTeachPlanDto(@PathVariable Long courseId) {
        return teachPlanService.queryTeachPlanDto(courseId);
    }

    @PostMapping(value = "/teachplan")
    @ApiOperation("课程计划信息修改/增加接口")
    public void saveTechPlanDto(@RequestBody SaveTeachplanDto saveTeachplanDto) {
        teachPlanService.saveTechPlanDto(saveTeachplanDto);
    }

    @ApiOperation("删除课程计划接口")
    @DeleteMapping("/teachplan/{id}")
    public void deleteTeachPlan(@PathVariable Long id) {
        teachPlanService.deleteTeachPlan(id);
    }

    @ApiOperation("课程排序接口")
    @PostMapping("/teachplan/{move}/{id}")
    public void moveTeachPlan(@PathVariable("move") String move, @PathVariable("id") Long id) {
        teachPlanService.moveTeachPlan(move, id);
    }
}
