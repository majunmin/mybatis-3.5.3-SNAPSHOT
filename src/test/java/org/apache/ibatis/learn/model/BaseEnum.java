package org.apache.ibatis.learn.model;

/**
 * 一句话功能简述 </br>
 *
 * @author majunmin
 * @description
 * @datetime 2019-09-17 20:43
 * @since
 */
public interface BaseEnum<E extends Enum<?>, T> {
  T getCode();

  E find(T t);

}
