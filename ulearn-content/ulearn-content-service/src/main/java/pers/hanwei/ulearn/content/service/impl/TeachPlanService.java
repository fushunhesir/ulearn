package pers.hanwei.ulearn.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pers.hanwei.ulearn.base.exception.TeachPlanDeleteException;
import pers.hanwei.ulearn.base.exception.ULearnException;
import pers.hanwei.ulearn.content.mapper.TeachplanMapper;
import pers.hanwei.ulearn.content.mapper.TeachplanMediaMapper;
import pers.hanwei.ulearn.content.model.dto.BindTeachplanMediaDto;
import pers.hanwei.ulearn.content.model.dto.SaveTeachplanDto;
import pers.hanwei.ulearn.content.model.dto.TeachplanDto;
import pers.hanwei.ulearn.content.model.po.Teachplan;
import pers.hanwei.ulearn.content.model.po.TeachplanMedia;
import pers.hanwei.ulearn.content.service.ITeachPlanService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeachPlanService implements ITeachPlanService {
    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;

    /**
     * @param courseId 课程id
     * @return 课程计划DTO
     */
    @Override
    public List<TeachplanDto> queryTeachPlanDto(Long courseId) {
        /*
        输入：课程id——根据id查询课程计划信息（三层树形结构）
        1. 找属于同一课程的所有节点
        2. 将一级节点添加到返回列表
        3. 遍历节点找到二级节点，插入到对应父亲的孩子节点中
        输出：课程章节信息列表
         */
        return teachplanMapper.selectTreeNodes(courseId);
    }

    /**
     * 保存课程计划
     * @param saveTeachplanDto 课程计划变化信息
     */
    @Override
    public void saveTechPlanDto(SaveTeachplanDto saveTeachplanDto) {
        /*
        输入：要保存的课程计划信息
        1. 判断是否有该课程的课程计划信息
        2. 如果没有：添加课程计划信息
        3. 如果有：修改课程计划信息
        输出：无
         */
        // 判断是否有该课程的课程计划
        // 根据id来获取
        if(saveTeachplanDto.getId() == null) {
            // 代表不存在该课程计划信息，插入新计划
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(saveTeachplanDto, teachplan);
            // 设置saveTeachplanDto中不存在的信息
            teachplan.setCreateDate(LocalDateTime.now());
            // 设置order
            teachplan.setOrderby(getOrder(saveTeachplanDto));
            teachplanMapper.insert(teachplan);
        } else {
            // 修改该课程的计划信息
            Teachplan teachplan = teachplanMapper.selectById(saveTeachplanDto.getId());
            BeanUtils.copyProperties(saveTeachplanDto, teachplan);
            // 修改其他信息，不用修改order
            teachplan.setChangeDate(LocalDateTime.now());
            teachplanMapper.updateById(teachplan);
        }
    }

    /**
     * 删除课程计划
     * @param id 课程计划id
     */
    @Override
    public void deleteTeachPlan(Long id) {
        /**
         * input: 课程id
         * 细节：删除时要更新order信息
         * output: 如果删除的是小节或无小节的章节 则无数据返回，否则，返回错误信息
         */
        // 根据id查询课程计划，并做检验
        Teachplan teachplan = teachplanMapper.selectById(id);
        if(teachplan == null) throw new ULearnException("该课程计划不存在");
        if(teachplan.getGrade() != null) {
            if(teachplan.getGrade() == 1) {
                // 删除的是章节
                if(hasChildren(teachplan)) {
                    throw new TeachPlanDeleteException("120409","课程计划还有子集信息，无法删除");
                } else {
                    teachplanMapper.deleteById(teachplan.getId());
                }
            }
            if(teachplan.getGrade() == 2) {
                teachplanMapper.deleteById(teachplan.getId());
            }
            updateOrder(teachplan.getCourseId(), teachplan.getGrade(), teachplan.getParentid());
        }
    }

    /**
     * 移动课程
     * @param move 移动方向
     * @param id   课程计划id
     */
    public void moveTeachPlan(String move, Long id) {
        /**
         * 输入：移动方向：向上，向下， 课程计划id
         * 输出：无
         * 过程：
         * 1. 根据id和方向，取出要交换的两个记录
         * 2. 交换order
         */
        Teachplan target = teachplanMapper.selectById(id);
        if(target == null) {
            throw new ULearnException("调整的课程计划不存在");
        } else {
            Teachplan exchange;
            LambdaQueryWrapper<Teachplan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Teachplan::getCourseId, target.getCourseId());
            wrapper.eq(Teachplan::getParentid, target.getParentid());
            if(move.equals("moveup")) {
                // 向上移动：取出要交换的课程计划记录
                wrapper.eq(Teachplan::getOrderby, target.getOrderby() - 1);
                exchange = teachplanMapper.selectOne(wrapper);
            } else {
                // 向下移动
                wrapper.eq(Teachplan::getOrderby, target.getOrderby() + 1);
                exchange = teachplanMapper.selectOne(wrapper);
            }
            if(exchange == null) throw new ULearnException("该计划无法执行该移动");
            else {
                int order = exchange.getOrderby();
                exchange.setOrderby(target.getOrderby());
                target.setOrderby(order);
                teachplanMapper.updateById(target);
                teachplanMapper.updateById(exchange);
            }
        }
    }


    /**
     * 将课程计划与媒体文件绑定
     * @param bindTeachplanMediaDto 前端提交的媒体文件信息——将该文件与课程计划绑定
     */
    @Override
    @Transactional
    public void associateMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {
        // 检测课程计划是否存在
        Teachplan teachplan = teachplanMapper.selectById(bindTeachplanMediaDto.getTeachplanId());
        if(teachplan == null) {
            throw new ULearnException("该课程计划不存在");
        }

        // 检测该课程计划是否是小节
        if(teachplan.getGrade() != 2) {
            throw new ULearnException("视频只能与小节绑定");
        }

        // 删除绑定表中该课程计划原有的信息
        teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>().eq(
                TeachplanMedia::getTeachplanId,
                bindTeachplanMediaDto.getTeachplanId()
        ));

        // 提取、补充信息
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        teachplanMedia.setCourseId(teachplan.getCourseId());
        teachplanMedia.setTeachplanId(teachplan.getId());
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        teachplanMedia.setMediaId(bindTeachplanMediaDto.getMediaId());
        teachplanMedia.setCreateDate(LocalDateTime.now());

        // 写回数据库
        teachplanMediaMapper.insert(teachplanMedia);
    }

    @Override
    public void inAssociateMedia(Long teachPlanId, String mediaId) {
        // 检测课程计划是否存在
        Teachplan teachplan = teachplanMapper.selectById(teachPlanId);
        if(teachplan == null) {
            throw new ULearnException("改课程计划不存在");
        }

        if(teachplan.getGrade() != 2) {
            throw new ULearnException("只能修改小节类型");
        }

        // 删除绑定表中该课程计划的信息
        LambdaQueryWrapper<TeachplanMedia> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(TeachplanMedia::getTeachplanId, teachPlanId);
        lambdaQueryWrapper.eq(TeachplanMedia::getMediaId, mediaId);
        teachplanMediaMapper.delete(lambdaQueryWrapper);
    }

    private void updateOrder(Long courseId, int grade, Long parentId) {
        /*
        功能：刷新课程计划的order
        输入：删除的课程计划id
        输出：无
        实现：1. 查表——该级目录的表
             2. 修改——遍历每一个表项修改
             3. 写回——将修改后的list写回
         */
        LambdaQueryWrapper<Teachplan> wrapper = new LambdaQueryWrapper<Teachplan>();
        wrapper.eq(Teachplan::getCourseId, courseId);
        wrapper.eq(Teachplan::getGrade, grade);
        wrapper.eq(Teachplan::getParentid, parentId);
        wrapper.orderByAsc(Teachplan::getOrderby);
        List<Teachplan> teachplans = teachplanMapper.selectList(wrapper);

        int i = 1;
        for(Teachplan e : teachplans) {
            e.setOrderby(i++);
            teachplanMapper.updateById(e);
        }
    }

    /**
     * 判断章节是否存在子小节
     * @param teachplan 查询的章节
     * @return 是否存在
     */
    private boolean hasChildren(Teachplan teachplan) {
        // 查询所有与该章节课程id相同且grade==2的小节
        LambdaQueryWrapper<Teachplan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Teachplan::getCourseId, teachplan.getCourseId());
        wrapper.eq(Teachplan::getGrade, 2);

        List<Teachplan> teachplans = teachplanMapper.selectList(wrapper);

        // 遍历查找是否有id等于查询章节id的
        List<Teachplan> res = teachplans.stream().filter(item->item.getParentid().equals(teachplan.getId())).collect(Collectors.toList());
        return !res.isEmpty();
    }

    private int getOrder(SaveTeachplanDto saveTeachplanDto) {

        LambdaQueryWrapper<Teachplan> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(Teachplan::getGrade, saveTeachplanDto.getGrade());
        wrapper.eq(Teachplan::getCourseId, saveTeachplanDto.getCourseId());

        List<Teachplan> teachPlans = teachplanMapper.selectList(wrapper);

        return (teachPlans.size() + 1);
    }
}
