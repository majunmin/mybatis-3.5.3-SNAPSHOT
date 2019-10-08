package org.apache.ibatis.learn.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ArticleTypeEnum implements BaseEnum<ArticleTypeEnum, Integer> {
  JAVA(1),
  DUBBO(2),
  SPRING(4),
  MYBATIS(8);

  private Integer code;


  @Override
  public ArticleTypeEnum find(Integer code) {
    for (ArticleTypeEnum at : ArticleTypeEnum.values()) {
      if (at.code == code) {
        return at;
      }
    }

    return null;
  }
}
