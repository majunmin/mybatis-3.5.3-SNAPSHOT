/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.reflection.wrapper;

import java.util.List;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * 对对象元信息的处理与封装
 * @author Clinton Begin
 */
public interface ObjectWrapper {

  /**
   * 如果ObjectWrapper 封装的是 普通javaBean , 则调用相应属性的 getter
   * 如果封装的是 集合类，则获取指定下标或者 key 对应的 value
   * @param prop
   * @return
   */
  Object get(PropertyTokenizer prop);

  /**
   * 如果ObjectWrapper 封装的是 普通javaBean , 则调用相应属性的 setter
   * 如果封装的是 集合类，则 设置 指定下标或者 key 对应的 value
   *
   * @param prop
   * @param value
   */
  void set(PropertyTokenizer prop, Object value);

  /**
   * 查找属性表达式对应的属性
   * @param name
   * @param useCamelCaseMapping 是否使用驼峰格式
   * @return
   */
  String findProperty(String name, boolean useCamelCaseMapping);

  String[] getGetterNames();

  String[] getSetterNames();

  Class<?> getSetterType(String name);

  Class<?> getGetterType(String name);

  boolean hasSetter(String name);

  boolean hasGetter(String name);

  /**
   * 为属性表达式的指定属性 创建相应的 MetaObject
   * @param name
   * @param prop
   * @param objectFactory
   * @return
   */
  MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory);

  // 判断对象类型是否是 Collection
  boolean isCollection();

  // 调用 Collection#add()
  void add(Object element);

  <E> void addAll(List<E> element);

}
