package org.apache.ibatis.learn.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章实体类 </br>
 *
 * @author majunmin
 * @description
 * @datetime 2019-09-17 16:21
 * @since
 */
@Data
public class Article {

  private Integer id;

  private String title;

  private String author;

  private String content;

  private LocalDateTime createTime;

  public Article(){

  }

  public Article(Integer id, String title, String content){
    this.id = id;
    this.title = title;
    this.content = content;
  }

}
