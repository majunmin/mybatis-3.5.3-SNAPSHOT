package org.apache.ibatis.learn.plugin;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PluginTest {

  private SqlSessionFactory sqlSessionFactory;

  @Before
  public void prepare() throws IOException {
    String resource = "org/apache/ibatis/learn/plugin/mybatis-config.xml";
    InputStream inputStream = Resources.getResourceAsStream(resource);
    sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    inputStream.close();
  }

  @Test
  public void testPage(){

    try(SqlSession sqlSession = sqlSessionFactory.openSession()){
      StudentDao mapper = sqlSession.getMapper(StudentDao.class);
      List<Student> byPaging = mapper.findByPaging(0, new RowBounds(1, 5));
      byPaging.forEach(System.out::println);
    }
  }
}
