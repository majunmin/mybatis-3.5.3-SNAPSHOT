package org.apache.ibatis.learn.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleDO {
    private Integer id;
    private String title;
    private ArticleTypeEnum type;
    private AuthorDO author;
    private String content;
    private LocalDateTime createTime;

    // 省略 getter/setter 和 toString
}
