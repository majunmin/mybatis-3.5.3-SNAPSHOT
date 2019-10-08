package org.apache.ibatis.learn;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.learn.model.Article;

import java.util.List;

/**
 * 一句话功能简述 </br>
 *
 * @author majunmin
 * @description
 * @datetime 2019-09-17 16:33
 * @since
 */
public interface ArticleMapper {

  List<Article> findByAuthorAndCreateTime(@Param("author") String author, @Param("createTime") String createTime);
}
