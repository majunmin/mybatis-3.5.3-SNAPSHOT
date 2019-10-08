package org.apache.ibatis.learn.learn2;

import org.apache.ibatis.learn.model.BaseEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomTypeHandler<E extends BaseEnum> extends BaseTypeHandler<E> {

  private Class<E> type;
  private E[] enums;

  public CustomTypeHandler(Class<E> type) {
    if (type == null)
      throw new IllegalArgumentException("Type argument cannot be null");
    this.type = type;
    this.enums = type.getEnumConstants();
    if (this.enums == null)
      throw new IllegalArgumentException(type.getSimpleName()
        + " does not represent an enum type.");
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType)
    throws SQLException {
    // 获取枚举的 code 值，并设置到 PreparedStatement 中
    ps.setObject(i, parameter.getCode());
  }

  @Override
  public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
    // 从 ResultSet 中获取 code
    int code = rs.getInt(columnName);
    // 解析 code 对应的枚举，并返回
    if (rs.wasNull()) {
      return null;
    } else {
      // 根据数据库中的value值，定位PersonType子类
      return locateEnumStatus(code);
    }
  }

  @Override
  public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    int code = rs.getInt(columnIndex);
    if (rs.wasNull()) {
      return null;
    } else {
      // 根据数据库中的value值，定位PersonType子类
      return locateEnumStatus(code);
    }
  }

  @Override
  public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    int code = cs.getInt(columnIndex);
    if (cs.wasNull()) {
      return null;
    } else {
      // 根据数据库中的value值，定位PersonType子类
      return locateEnumStatus(code);
    }
  }

  private E locateEnumStatus(int code) {
    for(E e : enums) {
      if(e.getCode().equals(code)) {
        return e;
      }
    }
    throw new IllegalArgumentException("未知的枚举类型：" + code + ",请核对" + type.getSimpleName());
  }

}
