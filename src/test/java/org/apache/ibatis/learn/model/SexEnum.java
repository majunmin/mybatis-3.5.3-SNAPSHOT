package org.apache.ibatis.learn.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SexEnum implements BaseEnum<SexEnum, Integer> {
  MAN(0),
  FEMALE(1),
  UNKNOWN(-1);

  private Integer code;

  @Override
  public SexEnum find(Integer integer) {
    for (SexEnum at : SexEnum.values()) {
      if (at.code == code) {
        return at;
      }
    }

    return null;
  }
}
