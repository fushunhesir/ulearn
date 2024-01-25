package pers.hanwei.ulearn.content.service;

import pers.hanwei.ulearn.content.model.dto.BindTeachplanMediaDto;
import pers.hanwei.ulearn.content.model.dto.SaveTeachplanDto;
import pers.hanwei.ulearn.content.model.dto.TeachplanDto;

import java.util.List;

public interface ITeachPlanService {
    List<TeachplanDto> queryTeachPlanDto(Long courseId);

    void saveTechPlanDto(SaveTeachplanDto saveTeachplanDto);

    void deleteTeachPlan(Long courseId);

    void moveTeachPlan(String move, Long id);

    void associateMedia(BindTeachplanMediaDto bindTeachplanMediaDto);

    void inAssociateMedia(Long teachPlanId, String mediaId);
}

