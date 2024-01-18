package pers.hanwei.ulearn.content.service;

import pers.hanwei.ulearn.content.model.dto.CourseCategoryTreeDto;

import java.util.List;

public interface ICourseCategoryService {
    List<CourseCategoryTreeDto> queryTreeNodes(String id);
}
