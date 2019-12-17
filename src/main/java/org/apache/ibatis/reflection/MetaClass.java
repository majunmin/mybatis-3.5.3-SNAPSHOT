/**
 *    Copyright 2009-2018 the original author or authors.
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
package org.apache.ibatis.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * 用来解析目标类的一些元信息，比如类的成员变量，getter/setter 方法等。
 * 通过 PropertyTokenizer 和 Reflector 组合使用， 解析复杂属性表达式，并实现了获取指定属性描述信息的功能
 *
 * @author Clinton Begin
 */
public class MetaClass {

  private final ReflectorFactory reflectorFactory;
  private final Reflector reflector;

  private MetaClass(Class<?> type, ReflectorFactory reflectorFactory) {
    /**
     * reflectorFactory Reflector 的工厂类, 有 创建&缓存 Reflector 的功能
     * reflector        反射器， 用于解析和存储目标类中的元信息
     */
    this.reflectorFactory = reflectorFactory;
    this.reflector = reflectorFactory.findForClass(type);
  }

  /**
   * 通过静态方法创建 MetaClass
   * @param type
   * @param
   * @return
   */
  public static MetaClass forClass(Class<?> type, ReflectorFactory reflectorFactory) {
    return new MetaClass(type, reflectorFactory);
  }

  public MetaClass metaClassForProperty(String name) {
    // 查找属性对应的 Class, 为该属性创建对应的 MetaClass
    Class<?> propType = reflector.getGetterType(name);
    return MetaClass.forClass(propType, reflectorFactory);
  }

  /**
   * 比较重要
   * 通过 调用 buildProperty(里面通过 PropertyTokenizer 进行复杂表达式的解析)
   * @param name
   * @return
   */
  public String findProperty(String name) {
    StringBuilder prop = buildProperty(name, new StringBuilder());
    return prop.length() > 0 ? prop.toString() : null;
  }

  public String findProperty(String name, boolean useCamelCaseMapping) {
    if (useCamelCaseMapping) {
      name = name.replace("_", "");
    }
    return findProperty(name);
  }

  public String[] getGetterNames() {
    return reflector.getGetablePropertyNames();
  }

  public String[] getSetterNames() {
    return reflector.getSetablePropertyNames();
  }

  public Class<?> getSetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      MetaClass metaProp = metaClassForProperty(prop.getName());
      return metaProp.getSetterType(prop.getChildren());
    } else {
      return reflector.getSetterType(prop.getName());
    }
  }

  public Class<?> getGetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      MetaClass metaProp = metaClassForProperty(prop);
      return metaProp.getGetterType(prop.getChildren());
    }
    // issue #506. Resolve the type inside a Collection Object
    return getGetterType(prop);
  }

  private MetaClass metaClassForProperty(PropertyTokenizer prop) {
    // 获取表达式所属的属性类型
    Class<?> propType = getGetterType(prop);
    return MetaClass.forClass(propType, reflectorFactory);
  }

  private Class<?> getGetterType(PropertyTokenizer prop) {
    Class<?> type = reflector.getGetterType(prop.getName());
    // 判断该表达式 是否含有 '[]'  && 是 Collection 的子类
    if (prop.getIndex() != null && Collection.class.isAssignableFrom(type)) {
      // 通过 TypeParameterResolver 工具类 解析属性的类型
      Type returnType = getGenericGetterType(prop.getName());
      // 针对 泛型集合类型的参数的处理
      if (returnType instanceof ParameterizedType) {
        // 获取实际的类型参数
        Type[] actualTypeArguments = ((ParameterizedType) returnType).getActualTypeArguments();
        if (actualTypeArguments != null && actualTypeArguments.length == 1) {
          returnType = actualTypeArguments[0];
          if (returnType instanceof Class) {
            type = (Class<?>) returnType;
          } else if (returnType instanceof ParameterizedType) {
            type = (Class<?>) ((ParameterizedType) returnType).getRawType();
          }
        }
      }
    }
    return type;
  }

  private Type getGenericGetterType(String propertyName) {
    try {
      // Reflector.getMethods 集合中记录了 Invoke 实现类的类型，决定解析 getter()返回值类型||字段类型
      Invoker invoker = reflector.getGetInvoker(propertyName);
      if (invoker instanceof MethodInvoker) {
        Field _method = MethodInvoker.class.getDeclaredField("method");
        _method.setAccessible(true);
        Method method = (Method) _method.get(invoker);
        return TypeParameterResolver.resolveReturnType(method, reflector.getType());
      } else if (invoker instanceof GetFieldInvoker) {
        Field _field = GetFieldInvoker.class.getDeclaredField("field");
        _field.setAccessible(true);
        Field field = (Field) _field.get(invoker);
        return TypeParameterResolver.resolveFieldType(field, reflector.getType());
      }
    } catch (NoSuchFieldException | IllegalAccessException ignored) {
    }
    return null;
  }

  /**
   * PropertyTokenizer : 属性分词器，用于处理较为复杂的属性名
   *
   * @param name
   * @return
   */
  public boolean hasSetter(String name) {
    // 属性 分词器, 用于解析属性名
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // prop.hasNext() == true 说明 name 是一个复合属性
    if (prop.hasNext()) {
      if (reflector.hasSetter(prop.getName())) {
        // 为属性 创建 MetaClass
        MetaClass metaProp = metaClassForProperty(prop.getName());
        // 再次调用 hasSetter()
        return metaProp.hasSetter(prop.getChildren());
      } else {
        return false;
      }
    } else {
      return reflector.hasSetter(prop.getName());
    }
  }

  /**
   * 调用 Reflector#hasGetter, 查找其 getMethods
   * @param name
   * @return
   */
  public boolean hasGetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      if (reflector.hasGetter(prop.getName())) {
        MetaClass metaProp = metaClassForProperty(prop);
        return metaProp.hasGetter(prop.getChildren());
      } else {
        return false;
      }
    } else {
      return reflector.hasGetter(prop.getName());
    }
  }

  public Invoker getGetInvoker(String name) {
    return reflector.getGetInvoker(name);
  }

  public Invoker getSetInvoker(String name) {
    return reflector.getSetInvoker(name);
  }

  private StringBuilder buildProperty(String name, StringBuilder builder) {
    // 解析属性表达式
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      //查找 prop.getName() 对应的属性
      String propertyName = reflector.findPropertyName(prop.getName());
      if (propertyName != null) {
        // 追加属性名
        builder.append(propertyName);
        builder.append(".");
        MetaClass metaProp = metaClassForProperty(propertyName);
        metaProp.buildProperty(prop.getChildren(), builder);
      }
    } else {
      // 递归出口
      String propertyName = reflector.findPropertyName(name);
      if (propertyName != null) {
        builder.append(propertyName);
      }
    }
    return builder;
  }

  public boolean hasDefaultConstructor() {
    return reflector.hasDefaultConstructor();
  }

}
