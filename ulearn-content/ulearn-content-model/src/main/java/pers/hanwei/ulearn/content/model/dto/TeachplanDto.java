package pers.hanwei.ulearn.content.model.dto;

import lombok.Data;
import lombok.ToString;
import pers.hanwei.ulearn.content.model.po.Teachplan;
import pers.hanwei.ulearn.content.model.po.TeachplanMedia;

import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description 课程计划信息模型类
 * @date 2023/2/14 11:23
 */
@Data
@ToString
public class TeachplanDto extends Teachplan {
  //与媒资管理的信息
   private TeachplanMedia teachplanMedia;

  //小章节list
   private List<TeachplanDto> teachPlanTreeNodes;
}
