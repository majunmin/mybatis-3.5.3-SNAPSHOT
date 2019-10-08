package org.apache.ibatis.learn.learn2;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.learn.model.ArticleDO;
import org.apache.ibatis.learn.model.AuthorDO;
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
 * @datetime 2019-09-17 20:23
 * @since
 */
public class MybatisTest {

  private SqlSessionFactory sqlSessionFactory;

  @Before
  public void prepare() throws IOException {
    String resource = "org/apache/ibatis/learn/learn2/mybatis-config.xml";
    InputStream inputStream = Resources.getResourceAsStream(resource);
    sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    inputStream.close();
  }

  @Test
  public void testMybatis(){

    try(SqlSession sqlSession = sqlSessionFactory.openSession()){

      ArticleDao articleDao = sqlSession.getMapper(ArticleDao.class);
      AuthorDao authorDao = sqlSession.getMapper(AuthorDao.class);
      ArticleDO articleDO = articleDao.findOne(1);

      AuthorDO authorDO = authorDao.findOne(1);

      System.out.println(articleDO);
      System.out.println(authorDO);
    }

  }


}
