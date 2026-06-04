package com.study.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.ai.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    /**
     * 根据关键词模糊检索知识库的标题或内容（保留旧方法兼容）
     */
    @Select("SELECT * FROM knowledge_base WHERE title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%')")
    List<KnowledgeBase> searchByKeyword(String keyword);

    /**
     * 使用 MySQL Ngram 全文索引检索知识库（按相关度排序，最多 3 条）
     */
    @Select("SELECT * FROM knowledge_base WHERE MATCH(title, content) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) ORDER BY MATCH(title, content) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) DESC LIMIT 3")
    List<KnowledgeBase> searchByFullText(@Param("keyword") String keyword);
}
