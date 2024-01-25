package pers.hanwei.ulearn.content.service.impl;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pers.hanwei.ulearn.base.exception.ULearnException;
import pers.hanwei.ulearn.content.mapper.CourseBaseMapper;
import pers.hanwei.ulearn.content.mapper.CourseMarketMapper;
import pers.hanwei.ulearn.content.mapper.CoursePublishMapper;
import pers.hanwei.ulearn.content.mapper.CoursePublishPreMapper;
import pers.hanwei.ulearn.content.model.dto.CourseBaseInfoDto;
import pers.hanwei.ulearn.content.model.dto.CoursePreviewDto;
import pers.hanwei.ulearn.content.model.dto.TeachplanDto;
import pers.hanwei.ulearn.content.model.po.CourseBase;
import pers.hanwei.ulearn.content.model.po.CourseMarket;
import pers.hanwei.ulearn.content.model.po.CoursePublish;
import pers.hanwei.ulearn.content.model.po.CoursePublishPre;
import pers.hanwei.ulearn.content.service.CoursePublishService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class CoursePublishServiceImpl implements CoursePublishService {
    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @Autowired
    TeachPlanService teachPlanService;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CoursePublishPreMapper coursePublishPreMapper;

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    CoursePublishMapper coursePublishMapper;


    /**
     * 根基课程id获取预览数据
     * @param courseId 课程id
     * @return 该课程的预览数据
     */
    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        // 获取基本信息、营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);

        // 获取课程计划信息
        List<TeachplanDto> teachplanDtos = teachPlanService.queryTeachPlanDto(courseId);

        // 生成返回信息
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfo);
        coursePreviewDto.setTeachplans(teachplanDtos);
        return coursePreviewDto;
    }


    /**
     *  提交审核：修改课程审核状态、将课程信息记录到预发布表中
     *  约束：
     * 1、对已提交审核的课程不允许提交审核。
     * 2、本机构只允许提交本机构的课程。
     * 3、没有上传图片不允许提交审核。
     * 4、没有添加课程计划不允许提交审核。
     * @param courseId 提交课程的ID
     * @param companyId 公司ID
     */
    @Override
    public void commitAudit(Long companyId, Long courseId) {
        // 检查课程是否存在
        CourseBaseInfoDto baseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        if(baseInfo == null) {
            throw new ULearnException("提交审核失败：该课程不存在");
        }

        // 检查课程状态：处于已提交状态的课程不能重复提交
        if(baseInfo.getAuditStatus().equals("202003")) {
            throw new ULearnException("提交审核失败：请勿重复提交");
        }

        // 检查课程图片是否存在
        if(baseInfo.getPic() == null || baseInfo.getPic().isEmpty()) {
            throw new ULearnException("提交审核失败：请上传课程图片");
        }

        // 检查课程计划是否存在
        List<TeachplanDto> teachplanDtos = teachPlanService.queryTeachPlanDto(courseId);
        if(teachplanDtos == null || teachplanDtos.isEmpty()) {
            throw new ULearnException("提交审核失败：请设置课程计划");
        }

        // 将课程信息添加到预发布表中
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(baseInfo, coursePublishPre);

        // 将课程营销信息添加到课程表预发布中
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        coursePublishPre.setMarket(JSON.toJSONString(courseMarket));

        // 将课程计划信息添加到课程预发布表中
        coursePublishPre.setTeachplan(JSON.toJSONString(teachplanDtos));

        // 设置其余信息
        coursePublishPre.setStatus("202003");
        coursePublishPre.setCompanyId(companyId);
        coursePublishPre.setCreateDate(LocalDateTime.now());

        // 更新预发布表
        if(coursePublishPreMapper.selectById(courseId) == null) {
            coursePublishPreMapper.insert(coursePublishPre);
        } else {
            coursePublishPreMapper.updateById(coursePublishPre);
        }

        // 更新基本信息表
        baseInfo.setAuditStatus("202003");
        courseBaseMapper.updateById(baseInfo);
    }

    /**
     * 发布课程服务
     * @param companyId 公司ID
     * @param courseId 课程ID
     */
    @Override
    public void publishCourse(Long companyId, Long courseId) {
        /**
         * 1、向课程发布表course_publish插入一条记录,记录来源于课程预发布表，如果存在则更新，发布状态为：已发布。
         * 2、更新course_base表的课程发布状态为：已发布
         * 3、删除课程预发布表的对应记录。
         * 4、向mq_message消息表插入一条消息，消息类型为：course_publish
         * 约束：
         * 1、课程审核通过方可发布。
         * 2、本机构只允许发布本机构的课程。
         */
        // 检查是否存在
        CoursePublishPre publishPre = coursePublishPreMapper.selectById(courseId);
        if(publishPre == null) {
            throw new ULearnException("发布课程失败：该课程未提交或已发布");
        }

        // 检查课程基本信息表中是否存在
        CourseBase baseInfo = courseBaseMapper.selectById(courseId);
        if(baseInfo == null) {
            throw new ULearnException("发布课程失败：该课程不在库中");
        }

        // 检查是否是本机构的课程
        if(!Objects.equals(publishPre.getCompanyId(), companyId)) {
            throw new ULearnException("发布课程失败：该课程不属于贵机构");
        }

        // 检查是否通过
        if(!publishPre.getStatus().equals("202004")) {
            throw new ULearnException("发布课程失败：该课程未通过或仍审核");
        }

        // 更新课程发布表
        CoursePublish publish = coursePublishMapper.selectById(courseId);
        if(publish == null) {
            publish = new CoursePublish();
            BeanUtils.copyProperties(publishPre, publish);
            publish.setStatus("203002");
            coursePublishMapper.insert(publish);
        } else {
            publish.setStatus("203002");
            coursePublishMapper.updateById(publish);
        }

        // 更新课程基本信息表
        baseInfo.setStatus("203002");
        courseBaseMapper.updateById(baseInfo);

        // 删除预发布表记录
        coursePublishPreMapper.deleteById(courseId);

        // 更新课程信息表
        
    }
}
