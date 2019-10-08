package org.apache.ibatis.learn;


import org.apache.ibatis.io.Resources;
import org.apache.ibatis.learn.model.Article;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MyBatisTest {

  private SqlSessionFactory sqlSessionFactory;

  @Before
  public void prepare() throws IOException {
    String resource = "org/apache/ibatis/learn/mybatis-config.xml";
    InputStream inputStream = Resources.getResourceAsStream(resource);
    sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    inputStream.close();
  }

  @Test
  public void testMyBatis() throws IOException {

    try (SqlSession session = sqlSessionFactory.openSession()) {
      ArticleMapper articleDao = session.getMapper(ArticleMapper.class);
      List<Article> articles = articleDao.findByAuthorAndCreateTime("majm", "2019-09-17 08:31:04");
      articles.forEach(System.out::println);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @Test
  public void testJDBC() throws IOException {
    String url = "jdbc:mysql://alima1:3306/mybatis-learning?user=root&password=root&useUnicode=true&characterEncoding=UTF8&useSSL=false";
    Connection conn = null;
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
      conn = DriverManager.getConnection(url);
      String author = "'majm'";
      String createTime = "'2018-09-18 08:00:00'";
      String sql = "select * from article where author =" + author + " and create_time > " + createTime;
      PreparedStatement statement = conn.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      List<Article> articleList = new ArrayList<>();
      while(rs.next()){
        Article article = new Article();

        articleList.add(article);
        int id = rs.getInt("id");
        String name = rs.getString("author");
        String title = rs.getString("title");
        String content = rs.getString("content");

        article.setId(id);
        article.setTitle(title);
        article.setAuthor(name);
        article.setContent(content);
//        article.setCreateTime(null);
      }

      articleList.forEach(System.out::println);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {

    }

  }
}
