//package org.apache.ibatis.learn.hibernate;
//
//import com.mysql.cj.Session;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//
//public class HibernateTest {
//
//    private SessionFactory buildSessionFactory;
//
//    @Before
//    public void init() {
//        Configuration configuration = new Configuration();
//        configuration.configure("hibernate.cfg.xml");
//        buildSessionFactory = configuration.buildSessionFactory();
//    }
//
//    @After
//    public void destroy() {
//        buildSessionFactory.close();
//    }
//
//    @Test
//    public void testORM() {
//        System.out.println("-----------------------------✨ ORM Query ✨--------------------------");
//
//        Session session = null;
//        try {
//            session = buildSessionFactory.openSession();
//            int id = 6;
//            Article article = session.get(Article.class, id);
//            System.out.println("ORM Query Result: ");
//            System.out.println(article);
//            System.out.println();
//        } finally {
//            if (Objects.nonNull(session)) {
//                session.close();
//            }
//        }
//
//    }
//
//    @Test
//    public void testHQL() {
//        System.out.println("-----------------------------✨ HQL Query ✨+--------------------------");
//        Session session = null;
//        try {
//            session = buildSessionFactory.openSession();
//            String hql = "from Article where author = :author and create_time > :createTime";
//            Query query = session.createQuery(hql);
//            query.setParameter("author", "coolblog.xyz");
//            query.setParameter("createTime", "2018.06.10");
//
//            List<Article> articles = query.list();
//            System.out.println("HQL Query Result: ");
//            articles.forEach(System.out::println);
//            System.out.println();
//        } finally {
//            if (Objects.nonNull(session)) {
//                session.close();
//            }
//        }
//    }
//
//    @Test
//    public void testJpaCriteria() throws ParseException {
//        System.out.println("---------------------------✨ JPA Criteria ✨------------------------");
//
//        Session session = null;
//        try {
//            session = buildSessionFactory.openSession();
//            CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
//            CriteriaQuery<Article> criteriaQuery = criteriaBuilder.createQuery(Article.class);
//
//            // 定义 FROM 子句
//            Root<Article> article = criteriaQuery.from(Article.class);
//
//            // 构建查询条件
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
//            Predicate greaterThan = criteriaBuilder.greaterThan(article.get("createTime"), sdf.parse("2018.06.10"));
//            Predicate equal = criteriaBuilder.equal(article.get("author"), "coolblog.xyz");
//
//            // 通过具有语义化的方法构建 SQL，等价于 SELECT ... FROM article WHERE ... AND ...
//            criteriaQuery.select(article).where(equal, greaterThan);
//
//            Query<Article> query = session.createQuery(criteriaQuery);
//            List<Article> articles = query.getResultList();
//
//            System.out.println("JPA Criteria Query Result: ");
//            articles.forEach(System.out::println);
//        } finally {
//            if (Objects.nonNull(session)) {
//                session.close();
//            }
//        }
//
//    }
//}
