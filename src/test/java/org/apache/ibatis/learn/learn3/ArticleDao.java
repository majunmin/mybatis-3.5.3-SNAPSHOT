package org.apache.ibatis.learn.learn3;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.learn.model.ArticleDO;

public interface ArticleDao {
    ArticleDO findOne(@Param("id") int id);
}
