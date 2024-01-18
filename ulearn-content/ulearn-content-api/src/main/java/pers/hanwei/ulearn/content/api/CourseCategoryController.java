package pers.hanwei.ulearn.content.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pers.hanwei.ulearn.content.model.dto.CourseCategoryTreeDto;
import pers.hanwei.ulearn.content.service.ICourseCategoryService;

import java.util.List;

@RestController
public class CourseCategoryController {
    @Autowired
    private ICourseCategoryService categoryService;

    @GetMapping("/course-category/tree-nodes")
    public List<CourseCategoryTreeDto> queryTreeNodes() {
        return categoryService.queryTreeNodes("1");
    }
}
