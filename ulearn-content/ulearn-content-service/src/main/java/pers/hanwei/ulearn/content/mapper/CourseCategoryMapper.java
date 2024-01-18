package pers.hanwei.ulearn.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import pers.hanwei.ulearn.content.model.dto.CourseCategoryTreeDto;
import pers.hanwei.ulearn.content.model.po.CourseCategory;

import java.util.List;

/**
 * <p>
 * 课程分类 Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface CourseCategoryMapper extends BaseMapper<CourseCategory> {

    //使用递归查询分类
    List<CourseCategoryTreeDto> queryTreeNodes(String id);
}
