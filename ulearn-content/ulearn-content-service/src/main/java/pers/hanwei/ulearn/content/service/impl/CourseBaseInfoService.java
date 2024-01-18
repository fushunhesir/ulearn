package pers.hanwei.ulearn.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import pers.hanwei.ulearn.base.exception.ULearnException;
import pers.hanwei.ulearn.base.model.PageParams;
import pers.hanwei.ulearn.base.model.PageResult;
import pers.hanwei.ulearn.content.mapper.CourseBaseMapper;
import pers.hanwei.ulearn.content.mapper.CourseCategoryMapper;
import pers.hanwei.ulearn.content.mapper.CourseMarketMapper;
import pers.hanwei.ulearn.content.model.dto.AddCourseDto;
import pers.hanwei.ulearn.content.model.dto.CourseBaseInfoDto;
import pers.hanwei.ulearn.content.model.dto.EditCourseDto;
import pers.hanwei.ulearn.content.model.dto.QueryCourseParamsDto;
import pers.hanwei.ulearn.content.model.po.CourseBase;
import pers.hanwei.ulearn.content.model.po.CourseCategory;
import pers.hanwei.ulearn.content.model.po.CourseMarket;
import pers.hanwei.ulearn.content.service.ICourseBaseInfoService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseBaseInfoService implements ICourseBaseInfoService {
    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Autowired
    private CourseMarketMapper courseMarketMapper;

    @Autowired
    private CourseCategoryMapper courseCategoryMapper;

    /**
     * 根据分页参数和课程属性查询数据
     * @param pageParams 分页查询参数
     * @param courseParamsDto 查询条件
     * @return PageResult<CourseBase> 返回课程基础属性查询结果
     */
    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto courseParamsDto) {
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        // 拼接查询条件
        // 设置对名字模糊查询
        queryWrapper.like(StringUtils.isNotEmpty(courseParamsDto.getCourseName()),
                CourseBase::getName,
                courseParamsDto.getCourseName());

        // 设置对课程审核状态相等查询
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getAuditStatus()),
                CourseBase::getAuditStatus,
                courseParamsDto.getAuditStatus());

        // 设置对课程发布状态相等查询
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getPublishStatus()),
                CourseBase::getStatus,
                courseParamsDto.getPublishStatus());

        // 设置分页参数
        IPage<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());

        // 根据查询条件和分页参数查询
        IPage<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);

        // 获取数据列表
        List<CourseBase> items = pageResult.getRecords();

        // 获取数据总条数
        Long total = pageResult.getTotal();

        // 设置返回数据
        return new PageResult<>(
                items, total, pageParams.getPageNo(), pageParams.getPageSize()
        );
    }

    /**
     * 获取课程基本信息
     * @param courseId 课程id
     * @return 前端需要的课程基本信息
     */
    @Override
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId) {
        // 实例化返回的DTO
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();

        // 根据courseId来获得课程基本信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if(courseBase == null) return null;
        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);

        // 根据courseId来获取课程营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        if(courseMarket != null) {
            BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);
        }
        CourseCategory courseCategoryBySt = courseCategoryMapper.selectById(courseBase.getSt());
        courseBaseInfoDto.setStName(courseCategoryBySt.getName());
        CourseCategory courseCategoryByMt = courseCategoryMapper.selectById(courseBase.getMt());
        courseBaseInfoDto.setMtName(courseCategoryByMt.getName());

        return courseBaseInfoDto;
    }

    /**
     * 添加课程
     * @param companyId 公司ID
     * @param addCourseDto 前端传过来的课程属性
     * @return 所添加课程的基本信息
     */
    @Override
    public CourseBaseInfoDto addCourseBaseInfo(Long companyId, AddCourseDto addCourseDto) {
        // 合法性校验：课程名称不为空、课程分类不为空、收费规则不为空、适用人群不为空
        if(StringUtils.isBlank(addCourseDto.getName())) throw new ULearnException("课程名称为空");
        if(StringUtils.isBlank(addCourseDto.getMt())
        || StringUtils.isBlank(addCourseDto.getSt()))
            throw new ULearnException("课程分类为空");
        if(StringUtils.isBlank(addCourseDto.getCharge())) throw new ULearnException("收费规则为空");
        if(StringUtils.isBlank(addCourseDto.getUsers())) throw new ULearnException("适用人群为空");

        // 利用mapper存储课程基本信息
        CourseBase newCourseBase = new CourseBase();
        BeanUtils.copyProperties(addCourseDto, newCourseBase);

        // 设置dto中缺少的基本信息：审核状态、发布状态、机构ID、插入时间
        // 这里不好，不应该硬编码审核状态和发布状态！！！
        newCourseBase.setAuditStatus("202002");
        newCourseBase.setStatus("203001");
        newCourseBase.setCompanyId(companyId);
        newCourseBase.setCreateDate(LocalDateTime.now());

        // 重复性检查后插入数据
        courseBaseMapper.insert(newCourseBase);

        // 设置课程营销信息
        CourseMarket newCourseMarket = new CourseMarket();
        BeanUtils.copyProperties(addCourseDto, newCourseMarket);
        newCourseMarket.setId(newCourseBase.getId());

        // 保存课程营销信息
        if(saveCourseMarket(newCourseMarket) <= 0) throw new ULearnException("保存课程营销信息失败");

        // 返回课程基本信息
        return getCourseBaseInfo(newCourseBase.getId());
    }

    private int saveCourseMarket(CourseMarket courseMarket) {
        // 合理性检验
        String charge = courseMarket.getCharge();
        if(StringUtils.isBlank(charge)){
            throw new ULearnException("收费规则没有选择");
        }
        if(charge.equals("201001") && (courseMarket.getPrice() == null
         || courseMarket.getPrice() <= 0)) {
            throw new ULearnException("价格必须为正数");
        }

        // 重复性检验
        CourseMarket oldCourseMarket = courseMarketMapper.selectById(courseMarket.getId());
        if(oldCourseMarket == null) {
            return courseMarketMapper.insert(courseMarket);
        } else {
            BeanUtils.copyProperties(courseMarket, oldCourseMarket);
            oldCourseMarket.setId(courseMarket.getId());
            return courseMarketMapper.updateById(oldCourseMarket);
        }
    }

    @Override
    public CourseBaseInfoDto editCourseBaseInfo(Long companyId, EditCourseDto editCourseDto) {
        /*
        思路：
        输入：公司Id——分辨不同公司的课程，避免修改其他公司的课程； 修改参数：修改后的数据
        1. 获取旧的课程基本信息 并根据公司id判断是否有修改权限
        2. 将editCourse中的对应数据复制到旧的课程信息中，写回数据
        3. 获取旧的课程营销信息
        4. 将editCourse中的对应数据复制到旧的课程营销信息中，写回数据
        5. 获取新的课程基本信息 以及 课程营销信息 拼接 为修改后的课程基本信息DTO
        输出：修改后的课程基本信息
         */
        CourseBase oldCourseBase = courseBaseMapper.selectById(editCourseDto.getId());
        CourseMarket oldCourseMarket = courseMarketMapper.selectById(editCourseDto.getId());
        if(oldCourseBase == null || oldCourseMarket == null) {
            throw new ULearnException("修改课程错误：课程不存在");
        }
        if(oldCourseBase.getCompanyId().equals(companyId)) {
            throw new ULearnException("此课程不属于贵公司");
        }

        BeanUtils.copyProperties(editCourseDto, oldCourseBase);
        oldCourseBase.setChangeDate(LocalDateTime.now());
        courseBaseMapper.updateById(oldCourseBase);

        BeanUtils.copyProperties(editCourseDto, oldCourseMarket);
        saveCourseMarket(oldCourseMarket);

        return this.getCourseBaseInfo(editCourseDto.getId());
    }
}
