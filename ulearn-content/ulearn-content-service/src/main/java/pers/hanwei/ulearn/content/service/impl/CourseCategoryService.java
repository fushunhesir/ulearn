package pers.hanwei.ulearn.content.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pers.hanwei.ulearn.content.mapper.CourseCategoryMapper;
import pers.hanwei.ulearn.content.model.dto.CourseCategoryTreeDto;
import pers.hanwei.ulearn.content.service.ICourseCategoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseCategoryService implements ICourseCategoryService {
    @Autowired
    CourseCategoryMapper categoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        // 从根节点获取整个树的表结构
        List<CourseCategoryTreeDto> categoryTreeDtos = categoryMapper.queryTreeNodes(id);
        // 创建从String到courseCategory的映射表
        Map<String, CourseCategoryTreeDto> mapTemp = categoryTreeDtos.stream().filter(
                item->!id.equals(item.getId())).collect(Collectors.toMap(
                        key -> key.getId(), value -> value,
                        (key1, key2) -> key2
                )
        );
        // 根据列表构造树形结构
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = new ArrayList<>();
        // 遍历列表，过滤根节点，将一级节点添加至结果列表，二级节点添加至对应父一级节点的孩子节点中
        categoryTreeDtos.stream().filter(item->!id.equals(item.getId())).forEach(
                item->{
                    // 添加一级节点至结果列表
                    if(item.getParentid().equals(id)) {
                        courseCategoryTreeDtos.add(item);
                    }
                    // 找到当前节点父节点，然后添加当前节点至父节点
                    CourseCategoryTreeDto parent = mapTemp.get(item.getParentid());
                    if(parent != null) {
                        if(parent.getChildrenTreeNodes() == null){
                            parent.setChildrenTreeNodes(new ArrayList<>());
                        }
                        parent.getChildrenTreeNodes().add(item);
                    }
                }
        );
        return courseCategoryTreeDtos;
    }
}
