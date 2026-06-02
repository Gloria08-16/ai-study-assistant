package com.study.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.study.ai.entity.KnowledgeBase;
import com.study.ai.mapper.KnowledgeBaseMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public KnowledgeController(KnowledgeBaseMapper knowledgeBaseMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
    }

    /**
     * 新增知识库资料
     */
    @PostMapping("/add")
    public KnowledgeBase add(@RequestBody KnowledgeBase knowledge) {
        knowledgeBaseMapper.insert(knowledge);
        return knowledge;
    }

    /**
     * 查询知识库列表，支持按标签筛选
     */
    @GetMapping("/list")
    public List<KnowledgeBase> list(@RequestParam(required = false) String tag) {
        if (tag != null && !tag.isBlank()) {
            return knowledgeBaseMapper.selectList(
                    new QueryWrapper<KnowledgeBase>().like("tags", tag));
        }
        return knowledgeBaseMapper.selectList(null);
    }

    /**
     * 删除知识库资料
     */
    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        knowledgeBaseMapper.deleteById(id);
        return "ok";
    }
}
