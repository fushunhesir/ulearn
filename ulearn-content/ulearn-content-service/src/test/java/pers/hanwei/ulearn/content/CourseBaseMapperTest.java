package pers.hanwei.ulearn.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pers.hanwei.ulearn.base.model.PageParams;
import pers.hanwei.ulearn.base.model.PageResult;
import pers.hanwei.ulearn.content.mapper.CourseBaseMapper;
import pers.hanwei.ulearn.content.model.dto.QueryCourseParamsDto;
import pers.hanwei.ulearn.content.model.po.CourseBase;

import java.util.List;

@SpringBootTest
public class CourseBaseMapperTest {
    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Test
    public void testCourseBaseMapper() {
        // 简单查询测试
        CourseBase courseBase = courseBaseMapper.selectById(74L);
        Assertions.assertNotNull(courseBase);

        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        // 设置查询参数
        QueryCourseParamsDto queryCourseParamsDto = new QueryCourseParamsDto();
        queryCourseParamsDto.setCourseName("java");
        queryCourseParamsDto.setPublishStatus("203001");
        queryCourseParamsDto.setAuditStatus("202004");

        // 拼接查询条件
        // 设置对名字模糊查询
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()),
                CourseBase::getName,
                queryCourseParamsDto.getCourseName());

        // 设置对课程审核状态相等查询
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()),
                CourseBase::getAuditStatus,
                queryCourseParamsDto.getAuditStatus());

        // 设置分页参数
        PageParams pageParams = new PageParams();
        pageParams.setPageNo(1L);
        pageParams.setPageSize(3L);
        IPage<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());

        // 根据查询条件和分页参数查询
        IPage<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);

        // 获取数据列表
        List<CourseBase> items = pageResult.getRecords();

        // 获取数据总条数
        Long total = pageResult.getTotal();

        // 设置返回数据
        PageResult<CourseBase> courseBasePageResult = new PageResult<>(
                items, total, pageParams.getPageNo(), pageParams.getPageSize()
        );
        System.out.println(courseBasePageResult);
    }
}
