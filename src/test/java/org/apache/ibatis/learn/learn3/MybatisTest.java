package org.apache.ibatis.learn.learn3;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.learn.learn2.ArticleDao;
import org.apache.ibatis.learn.learn2.AuthorDao;
import org.apache.ibatis.learn.model.ArticleDO;
import org.apache.ibatis.learn.model.AuthorDO;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * 一句话功能简述 </br>
 *
 * @author majunmin
 * @description
 * @datetime 2019-09-21 11:04
 * @since
 */
public class MybatisTest {

  private SqlSessionFactory sqlSessionFactory;

  @Before
  public void prepare() throws IOException {
    String resource = "org/apache/ibatis/learn/learn3/mybatis-config.xml";
    InputStream inputStream = Resources.getResourceAsStream(resource);
    sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    inputStream.close();
  }

  @Test
  public void testParseSql(){
    try(SqlSession sqlSession = sqlSessionFactory.openSession()){

      org.apache.ibatis.learn.learn2.ArticleDao articleDao = sqlSession.getMapper(ArticleDao.class);
      ArticleDO articleDO = articleDao.findOne(1);


      System.out.println(articleDO);
    }
  }

  @Test
  public void printResultMapInfo() throws Exception {
    Configuration configuration = new Configuration();
    String resource = "org/apache/ibatis/learn/learn3/ArticleMapper.xml";
    InputStream inputStream = Resources.getResourceAsStream(resource);
    XMLMapperBuilder builder = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
    builder.parse();

    ResultMap resultMap = configuration.getResultMap("articleResult");

    System.out.println("\n-------------------+✨ mappedColumns ✨+--------------------");
    System.out.println(resultMap.getMappedColumns());

    System.out.println("\n------------------+✨ mappedProperties ✨+------------------");
    System.out.println(resultMap.getMappedProperties());

    System.out.println("\n------------------+✨ idResultMappings ✨+------------------");
    resultMap.getIdResultMappings().forEach(rm -> System.out.println(simplify(rm)));

    System.out.println("\n---------------+✨ propertyResultMappings ✨+---------------");
    resultMap.getPropertyResultMappings().forEach(rm -> System.out.println(simplify(rm)));

    System.out.println("\n-------------+✨ constructorResultMappings ✨+--------------");
    resultMap.getConstructorResultMappings().forEach(rm -> System.out.println(simplify(rm)));

    System.out.println("\n-------------------+✨ resultMappings ✨+-------------------");
    resultMap.getResultMappings().forEach(rm -> System.out.println(simplify(rm)));

    inputStream.close();
  }

  /** 简化 ResultMapping 输出结果 */
  private String simplify(ResultMapping resultMapping) {
    return String.format("ResultMapping{column='%s', property='%s', flags=%s, ...}",
      resultMapping.getColumn(), resultMapping.getProperty(), resultMapping.getFlags());
  }


}
