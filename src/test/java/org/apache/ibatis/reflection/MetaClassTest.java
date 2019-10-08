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
package org.apache.ibatis.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import lombok.Data;
import org.apache.ibatis.domain.misc.RichType;
import org.apache.ibatis.domain.misc.generics.GenericConcrete;
import org.junit.jupiter.api.Test;

class MetaClassTest {

  @Test
  void shouldTestDataTypeOfGenericMethod() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    MetaClass meta = MetaClass.forClass(GenericConcrete.class, reflectorFactory);
    assertEquals(Long.class, meta.getGetterType("id"));
    assertEquals(Long.class, meta.getSetterType("id"));
  }

  @Test
  void shouldThrowReflectionExceptionGetGetterType() {
    try {
      ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
      MetaClass meta = MetaClass.forClass(RichType.class, reflectorFactory);
      meta.getGetterType("aString");
      org.junit.jupiter.api.Assertions.fail("should have thrown ReflectionException");
    } catch (ReflectionException expected) {
      assertEquals("There is no getter for property named \'aString\' in \'class org.apache.ibatis.domain.misc.RichType\'", expected.getMessage());
    }
  }

  @Test
  void shouldCheckGetterExistance() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    MetaClass meta = MetaClass.forClass(RichType.class, reflectorFactory);
    assertTrue(meta.hasGetter("richField"));
    assertTrue(meta.hasGetter("richProperty"));
    assertTrue(meta.hasGetter("richList"));
    assertTrue(meta.hasGetter("richMap"));
    assertTrue(meta.hasGetter("richList[0]"));

    assertTrue(meta.hasGetter("richType"));
    assertTrue(meta.hasGetter("richType.richField"));
    assertTrue(meta.hasGetter("richType.richProperty"));
    assertTrue(meta.hasGetter("richType.richList"));
    assertTrue(meta.hasGetter("richType.richMap"));
    assertTrue(meta.hasGetter("richType.richList[0]"));

    assertEquals("richType.richProperty", meta.findProperty("richType.richProperty", false));

    assertFalse(meta.hasGetter("[0]"));
  }

  @Test
  void shouldCheckSetterExistance() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    MetaClass meta = MetaClass.forClass(RichType.class, reflectorFactory);
    assertTrue(meta.hasSetter("richField"));
    assertTrue(meta.hasSetter("richProperty"));
    assertTrue(meta.hasSetter("richList"));
    assertTrue(meta.hasSetter("richMap"));
    assertTrue(meta.hasSetter("richList[0]"));

    assertTrue(meta.hasSetter("richType"));
    assertTrue(meta.hasSetter("richType.richField"));
    assertTrue(meta.hasSetter("richType.richProperty"));
    assertTrue(meta.hasSetter("richType.richList"));
    assertTrue(meta.hasSetter("richType.richMap"));
    assertTrue(meta.hasSetter("richType.richList[0]"));

    assertFalse(meta.hasSetter("[0]"));
  }

  @Test
  void shouldCheckTypeForEachGetter() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    MetaClass meta = MetaClass.forClass(RichType.class, reflectorFactory);
    assertEquals(String.class, meta.getGetterType("richField"));
    assertEquals(String.class, meta.getGetterType("richProperty"));
    assertEquals(List.class, meta.getGetterType("richList"));
    assertEquals(Map.class, meta.getGetterType("richMap"));
    assertEquals(List.class, meta.getGetterType("richList[0]"));

    assertEquals(RichType.class, meta.getGetterType("richType"));
    assertEquals(String.class, meta.getGetterType("richType.richField"));
    assertEquals(String.class, meta.getGetterType("richType.richProperty"));
    assertEquals(List.class, meta.getGetterType("richType.richList"));
    assertEquals(Map.class, meta.getGetterType("richType.richMap"));
    assertEquals(List.class, meta.getGetterType("richType.richList[0]"));
  }

  @Test
  void shouldCheckTypeForEachSetter() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    MetaClass meta = MetaClass.forClass(RichType.class, reflectorFactory);
    assertEquals(String.class, meta.getSetterType("richField"));
    assertEquals(String.class, meta.getSetterType("richProperty"));
    assertEquals(List.class, meta.getSetterType("richList"));
    assertEquals(Map.class, meta.getSetterType("richMap"));
    assertEquals(List.class, meta.getSetterType("richList[0]"));

    assertEquals(RichType.class, meta.getSetterType("richType"));
    assertEquals(String.class, meta.getSetterType("richType.richField"));
    assertEquals(String.class, meta.getSetterType("richType.richProperty"));
    assertEquals(List.class, meta.getSetterType("richType.richList"));
    assertEquals(Map.class, meta.getSetterType("richType.richMap"));
    assertEquals(List.class, meta.getSetterType("richType.richList[0]"));
  }

  @Test
  void shouldCheckGetterAndSetterNames() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    MetaClass meta = MetaClass.forClass(RichType.class, reflectorFactory);
    assertEquals(5, meta.getGetterNames().length);
    assertEquals(5, meta.getSetterNames().length);
  }

  @Test
  void shouldFindPropertyName() {
    ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
    MetaClass meta = MetaClass.forClass(RichType.class, reflectorFactory);
    assertEquals("richField", meta.findProperty("RICHfield"));
  }


  @Data
  private class Author {
    private Integer id;
    private String name;
    private Integer age;
    /** 一个作者对应多篇文章 */
    private Article[] articles;
  }

  @Data
  private class Article {
    private Integer id;
    private String title;
    private String content;
    /** 一篇文章对应一个作者 */
    private Author author;
  }
  @Test
  public void testCustom(){
    MetaClass authorMetaClass = MetaClass.forClass(Author.class, new DefaultReflectorFactory());
    System.out.println("======Author========");
    System.out.println("id : " + authorMetaClass.hasSetter("id"));
    System.out.println("name : " + authorMetaClass.hasSetter("name"));
    System.out.println("age : " + authorMetaClass.hasSetter("age"));
    System.out.println("articles : " + authorMetaClass.hasSetter("articles"));

    System.out.println("articles[] : " + authorMetaClass.hasSetter("articles[]"));
    System.out.println("title : " + authorMetaClass.hasSetter("title"));

    MetaClass articleMetaClass = MetaClass.forClass(Article.class, new DefaultReflectorFactory());
    System.out.println("======Article========");
    System.out.println("id : " + articleMetaClass.hasSetter("id"));
    System.out.println("title : " + articleMetaClass.hasSetter("title"));
    System.out.println("content : " + articleMetaClass.hasSetter("content"));
    System.out.println("author : " + articleMetaClass.hasSetter("author"));
    System.out.println("author.age : " + articleMetaClass.hasSetter("author.age"));

    System.out.println(MetaClass.class.getName());
    System.out.println(MetaClass.class.getSimpleName());
    System.out.println(MetaClass.class.getTypeName());
  }

}
