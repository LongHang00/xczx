package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseCategoryServiceImpl implements CourseCategoryService {

    private final CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        //调用mapper递归查询出结果
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);
        //找到每个节点的子节点，最终封装成CourseCategoryTreeDto
        //先将list转为map<CourseCategoryTreeDto>，一边遍历一边找子节点放在父节点的childrenTreeNodes
        Map<String, CourseCategoryTreeDto> collect = courseCategoryTreeDtos.stream().filter(item -> !id.equals(item.getId())).collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));
        // 先定义一个最终返回的list
        List<CourseCategoryTreeDto> courseCategoryPrent = new ArrayList<>();
        //从头遍历List
        courseCategoryTreeDtos.stream().filter(item -> !id.equals(item.getId())).forEach(item -> {
            CourseCategoryTreeDto courseCategoryTreeDto = collect.get(item.getParentid());
            if (item.getParentid().equals(id)) {
                courseCategoryPrent.add(item);
            }
            if (courseCategoryTreeDto!=null) {
                if (courseCategoryTreeDto.getChildrenTreeNodes() == null) {
                    courseCategoryTreeDto.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                }
                courseCategoryTreeDto.getChildrenTreeNodes().add(item);
            }
        });
        return courseCategoryPrent;
    }
}
