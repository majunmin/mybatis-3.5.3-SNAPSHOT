package org.apache.ibatis.learn.model;

import lombok.Data;

import java.util.List;

/**
 * 一句话功能简述 </br>
 *
 * @author majunmin
 * @description
 * @datetime 2019-09-17 19:25
 * @since
 */
@Data
public class AuthorDO {
  private Integer id;
  private String name;
  private Integer age;
  private Integer sex;
  private String email;
  private List<ArticleDO> articles;
}
