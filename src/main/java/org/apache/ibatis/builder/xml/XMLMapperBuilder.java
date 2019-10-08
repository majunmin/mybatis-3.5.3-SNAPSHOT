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
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class XMLMapperBuilder extends BaseBuilder {

  private final XPathParser parser;
  private final MapperBuilderAssistant builderAssistant;
  private final Map<String, XNode> sqlFragments;
  private final String resource;

  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
    this(reader, configuration, resource, sqlFragments);
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    this(new XPathParser(reader, true, configuration.getVariables(), new XMLMapperEntityResolver()),
        configuration, resource, sqlFragments);
  }

  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
    this(inputStream, configuration, resource, sqlFragments);
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    this(new XPathParser(inputStream, true, configuration.getVariables(), new XMLMapperEntityResolver()),
        configuration, resource, sqlFragments);
  }

  private XMLMapperBuilder(XPathParser parser, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    // 将 configuration 赋值给 BaseBuilder
    super(configuration);
    this.builderAssistant = new MapperBuilderAssistant(configuration, resource);
    this.parser = parser;
    this.sqlFragments = sqlFragments;
    this.resource = resource;
  }

  public void parse() {
    //若当前的 Mapper.xml 尚未被解析， 则开始解析
    // PS: 若 <mappers> 节点下有相同的 <mapper> 节点，就无序再次解析
    if (!configuration.isResourceLoaded(resource)) {
      configurationElement(parser.evalNode("/mapper"));
      // 将解析的 mapper 节点 添加到 configuration 的 loadedResources 中，下次无需再次解析
      configuration.addLoadedResource(resource);
      // 将 mapper.xml 对应的 MapperClass 注册进 configuration 的 mapperRegistry
      bindMapperForNamespace();
    }

    // 处理未完成解析的节点
    parsePendingResultMaps();
    parsePendingCacheRefs();
    parsePendingStatements();
  }

  public XNode getSqlFragment(String refid) {
    return sqlFragments.get(refid);
  }

  /**
   * 定义了 mapper.xml 文件的解析流程
   * @param context
   */
  private void configurationElement(XNode context) {
    try {
      // 获取 <mapper> 节点的 namespace 属性， 该属性必须存在，表示当前映射文件对应的 MapperClass 是谁
      String namespace = context.getStringAttribute("namespace");
      if (namespace == null || namespace.equals("")) {
        throw new BuilderException("Mapper's namespace cannot be empty");
      }
      // 将namespace属性值赋给builderAssistant
      builderAssistant.setCurrentNamespace(namespace);
      cacheRefElement(context.evalNode("cache-ref"));
      cacheElement(context.evalNode("cache"));
      parameterMapElement(context.evalNodes("/mapper/parameterMap"));
      // 解析 <resultMap>
      resultMapElements(context.evalNodes("/mapper/resultMap"));
      sqlElement(context.evalNodes("/mapper/sql"));
      // 解析sql语句
      buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
    }
  }

  private void buildStatementFromContext(List<XNode> list) {
    if (configuration.getDatabaseId() != null) {
      buildStatementFromContext(list, configuration.getDatabaseId());
    }
    buildStatementFromContext(list, null);
  }

  private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
    for (XNode context : list) {
      // 创建 XMLStatementBuilder 构建类
      final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, context, requiredDatabaseId);
      try {
        /**
         * 解析 Statement 节点，并将解析结果存储到
         * configuration 的 mappedStatements 集合中
         */
        statementParser.parseStatementNode();
      } catch (IncompleteElementException e) {
        // 解析失败，将解析器放入 configuration 的 incompleteStatements 集合中
        configuration.addIncompleteStatement(statementParser);
      }
    }
  }

  private void parsePendingResultMaps() {
    Collection<ResultMapResolver> incompleteResultMaps = configuration.getIncompleteResultMaps();
    synchronized (incompleteResultMaps) {
      Iterator<ResultMapResolver> iter = incompleteResultMaps.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().resolve();
          iter.remove();
        } catch (IncompleteElementException e) {
          // ResultMap is still missing a resource...
        }
      }
    }
  }

  private void parsePendingCacheRefs() {
    // 获取 CacheRefResolver 列表
    Collection<CacheRefResolver> incompleteCacheRefs = configuration.getIncompleteCacheRefs();
    synchronized (incompleteCacheRefs) {
      Iterator<CacheRefResolver> iter = incompleteCacheRefs.iterator();
      while (iter.hasNext()) {
        try {
          /**
           * 尝试解析 <cache-ref> 节点，若解析失败，则抛出 IncompleteElementException，
           * 此时下面的删除操作不会被执行
           */
          iter.next().resolveCacheRef();
          iter.remove();
        } catch (IncompleteElementException e) {
          // Cache ref is still missing a resource...

          /**
           * 如果再次发生 IncompleteElementException 异常，表明当前映射文件中并没有
           * <cache-ref> 所引用的缓存。有可能所引用的缓存在后面的映射文件中，所以这里
           * 不能将解析失败的 CacheRefResolver 从集合中删除
           */
        }
      }
    }
  }

  private void parsePendingStatements() {
    Collection<XMLStatementBuilder> incompleteStatements = configuration.getIncompleteStatements();
    synchronized (incompleteStatements) {
      Iterator<XMLStatementBuilder> iter = incompleteStatements.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().parseStatementNode();
          iter.remove();
        } catch (IncompleteElementException e) {
          // Statement is still missing a resource...
        }
      }
    }
  }

  private void cacheRefElement(XNode context) {
    if (context != null) {
      configuration.addCacheRef(builderAssistant.getCurrentNamespace(), context.getStringAttribute("namespace"));
      // 创建 CacheRefResolver
      CacheRefResolver cacheRefResolver = new CacheRefResolver(builderAssistant, context.getStringAttribute("namespace"));
      try {
        // 解析参照缓存
        cacheRefResolver.resolveCacheRef();
      } catch (IncompleteElementException e) {
        /**
         * 这里对 IncompleteElementException 异常进行捕捉，并将 cacheRefResolver
         * 存入到 Configuration 的 incompleteCacheRefs 集合中
         */
        configuration.addIncompleteCacheRef(cacheRefResolver);
      }
    }
  }

  /**
   * Mybatis  提供了 一 二 级 缓存， 其中一级缓存 是 sqlSession级别， 默认是开启状态。
   * 二级缓存配置在映射文件中，使用者需要显示配置才能开启。
   * ```
   * <cache/>
   * ```
   * ```
   * <cache eviction="FIFO" flushInterval="60000" size="512" readOnly="true"/>
   * ```
   *
   * @param context
   */
  private void cacheElement(XNode context) {
    if (context != null) {
      String type = context.getStringAttribute("type", "PERPETUAL");
      Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
      String eviction = context.getStringAttribute("eviction", "LRU");
      Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
      Long flushInterval = context.getLongAttribute("flushInterval");
      Integer size = context.getIntAttribute("size");
      boolean readWrite = !context.getBooleanAttribute("readOnly", false);
      boolean blocking = context.getBooleanAttribute("blocking", false);
      Properties props = context.getChildrenAsProperties();
      // 构建缓存对象
      builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
    }
  }

  private void parameterMapElement(List<XNode> list) {
    for (XNode parameterMapNode : list) {
      String id = parameterMapNode.getStringAttribute("id");
      String type = parameterMapNode.getStringAttribute("type");
      Class<?> parameterClass = resolveClass(type);
      List<XNode> parameterNodes = parameterMapNode.evalNodes("parameter");
      List<ParameterMapping> parameterMappings = new ArrayList<>();
      for (XNode parameterNode : parameterNodes) {
        String property = parameterNode.getStringAttribute("property");
        String javaType = parameterNode.getStringAttribute("javaType");
        String jdbcType = parameterNode.getStringAttribute("jdbcType");
        String resultMap = parameterNode.getStringAttribute("resultMap");
        String mode = parameterNode.getStringAttribute("mode");
        String typeHandler = parameterNode.getStringAttribute("typeHandler");
        Integer numericScale = parameterNode.getIntAttribute("numericScale");
        ParameterMode modeEnum = resolveParameterMode(mode);
        Class<?> javaTypeClass = resolveClass(javaType);
        JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
        Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
        ParameterMapping parameterMapping = builderAssistant.buildParameterMapping(parameterClass, property, javaTypeClass, jdbcTypeEnum, resultMap, modeEnum, typeHandlerClass, numericScale);
        parameterMappings.add(parameterMapping);
      }
      builderAssistant.addParameterMap(id, parameterClass, parameterMappings);
    }
  }

  /**
   * 该函数用于解析映射文件中所有的<resultMap>节点，
   * 这些节点会被解析成ResultMap对象，存储在Configuration对象的resultMaps容器中。
   * @param list
   * @throws Exception
   */
  private void resultMapElements(List<XNode> list) throws Exception {
    for (XNode resultMapNode : list) {
      try {
        resultMapElement(resultMapNode);
      } catch (IncompleteElementException e) {
        // ignore, it will be retried
      }
    }
  }

  private ResultMap resultMapElement(XNode resultMapNode) throws Exception {
    return resultMapElement(resultMapNode, Collections.emptyList(), null);
  }

  private ResultMap resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings, Class<?> enclosingType) throws Exception {
    ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());
    // 获取<ResultMap>上的type属性（即resultMap的返回值类型）
    String type = resultMapNode.getStringAttribute("type",
        resultMapNode.getStringAttribute("ofType",
            resultMapNode.getStringAttribute("resultType",
                resultMapNode.getStringAttribute("javaType"))));
    // 将 resultMap 的返回值类型转换成Class对象
    Class<?> typeClass = resolveClass(type);
    if (typeClass == null) {
      typeClass = inheritEnclosingType(resultMapNode, enclosingType);
    }
    Discriminator discriminator = null;
    // resultMappings用于存储<resultMap>下所有的子节点
    List<ResultMapping> resultMappings = new ArrayList<>();
    resultMappings.addAll(additionalResultMappings);
    // 获取并遍历<resultMap>下所有的子节点
    List<XNode> resultChildren = resultMapNode.getChildren();
    for (XNode resultChild : resultChildren) {
      // 若当前节点为<constructor>，则将它的子节点们添加到resultMappings中去
      if ("constructor".equals(resultChild.getName())) {
        processConstructorElement(resultChild, typeClass, resultMappings);
      } else if ("discriminator".equals(resultChild.getName())) {
        // 若当前节点为<discriminator>，则进行条件判断，并将命中的子节点添加到resultMappings中去
        discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
      } else {
        // 若当前节点为<result>、<association>、<collection>，则将其添加到resultMappings中去
        // PS:flags仅用于区分当前节点是否是<id>或<idArg>，因为这两个节点的属性名为name，而其他节点的属性名为property
        List<ResultFlag> flags = new ArrayList<>();
        if ("id".equals(resultChild.getName())) {
          flags.add(ResultFlag.ID);
        }
        resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
      }
    }
    String id = resultMapNode.getStringAttribute("id",
            resultMapNode.getValueBasedIdentifier());
    String extend = resultMapNode.getStringAttribute("extends");
    Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");

    // ResultMapResolver的作用是生成ResultMap对象，并将其加入到Configuration对象的resultMaps容器中（具体过程见下）
    ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend, discriminator, resultMappings, autoMapping);
    try {
      return resultMapResolver.resolve();
    } catch (IncompleteElementException  e) {
      configuration.addIncompleteResultMap(resultMapResolver);
      throw e;
    }
  }

  protected Class<?> inheritEnclosingType(XNode resultMapNode, Class<?> enclosingType) {
    if ("association".equals(resultMapNode.getName()) && resultMapNode.getStringAttribute("resultMap") == null) {
      String property = resultMapNode.getStringAttribute("property");
      if (property != null && enclosingType != null) {
        MetaClass metaResultType = MetaClass.forClass(enclosingType, configuration.getReflectorFactory());
        return metaResultType.getSetterType(property);
      }
    } else if ("case".equals(resultMapNode.getName()) && resultMapNode.getStringAttribute("resultMap") == null) {
      return enclosingType;
    }
    return null;
  }

  private void processConstructorElement(XNode resultChild, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
    List<XNode> argChildren = resultChild.getChildren();
    for (XNode argChild : argChildren) {
      List<ResultFlag> flags = new ArrayList<>();
      flags.add(ResultFlag.CONSTRUCTOR);
      if ("idArg".equals(argChild.getName())) {
        flags.add(ResultFlag.ID);
      }
      resultMappings.add(buildResultMappingFromContext(argChild, resultType, flags));
    }
  }

  private Discriminator processDiscriminatorElement(XNode context, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String typeHandler = context.getStringAttribute("typeHandler");
    Class<?> javaTypeClass = resolveClass(javaType);
    Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
    Map<String, String> discriminatorMap = new HashMap<>();
    for (XNode caseChild : context.getChildren()) {
      String value = caseChild.getStringAttribute("value");
      String resultMap = caseChild.getStringAttribute("resultMap", processNestedResultMappings(caseChild, resultMappings, resultType));
      discriminatorMap.put(value, resultMap);
    }
    return builderAssistant.buildDiscriminator(resultType, column, javaTypeClass, jdbcTypeEnum, typeHandlerClass, discriminatorMap);
  }

  private void sqlElement(List<XNode> list) {
    if (configuration.getDatabaseId() != null) {
      // 第一次传入具体的 databaseId，用于解析带有 databaseId 属性
      sqlElement(list, configuration.getDatabaseId());
    }
    // 第二次传入的 databaseId 为空，用于解析未配置 databaseId 属性的 <sql> 节点
    sqlElement(list, null);
  }

  private void sqlElement(List<XNode> list, String requiredDatabaseId) {
    for (XNode context : list) {
      String databaseId = context.getStringAttribute("databaseId");
      String id = context.getStringAttribute("id");
      // id = currentNameSpace + "." + id
      id = builderAssistant.applyCurrentNamespace(id, false);
      // 检测当前 databaseId 和 requiredDatabaseId 是否一致
      if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
        // 将 id -> Xnode 键值对 添加到 sqlFragments
        sqlFragments.put(id, context);
      }
    }
  }

  /**
   * 关于 databaseId 的用途，这里简单介绍一下。databaseId 用于标明数据库厂商的身份，不同厂商有自己的 SQL 方言，MyBatis 可以根据 databaseId 执行不同 SQL 语句。
   * databaseId 在 <sql> 节点中有什么用呢？这个问题也不难回答。
   * <sql> 节点用于保存 SQL 语句片段，如果 SQL 语句片段中包含方言的话，那么该 <sql> 节点只能被同一 databaseId 的查询语句或更新语句引用。
   *
   * @param id
   * @param databaseId
   * @param requiredDatabaseId
   * @return
   */
  private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
    if (requiredDatabaseId != null) {
      return requiredDatabaseId.equals(databaseId);
    }
    if (databaseId != null) {
      return false;
    }
    if (!this.sqlFragments.containsKey(id)) {
      return true;
    }
    // skip this fragment if there is a previous one with a not null databaseId
    XNode context = this.sqlFragments.get(id);
    return context.getStringAttribute("databaseId") == null;
  }

  /**
   * 上面的方法主要用于获取 <id> 和 <result> 节点的属性，
   * 其中，resultMap 属性的解析过程要相对复杂一些。该属性存在于 <association> 和 <collection> 节点中。
   *
   * ```xml
   * <resultMap id="articleResult" type="Article">
   *     <id property="id" column="id"/>
   *     <result property="title" column="article_title"/>
   *     <!-- 引用 authorResult -->
   *     <association property="article_author" column="article_author_id" javaType="Author" resultMap="authorResult"/>
   * </resultMap>
   *
   * <resultMap id="authorResult" type="Author">
   *     <id property="id" column="author_id"/>
   *     <result property="name" column="author_name"/>
   * </resultMap>
   * ```
   * @param context
   * @param resultType
   * @param flags
   * @return
   * @throws Exception
   */
  private ResultMapping buildResultMappingFromContext(XNode context, Class<?> resultType, List<ResultFlag> flags) throws Exception {
    String property;
    if (flags.contains(ResultFlag.CONSTRUCTOR)) {
      property = context.getStringAttribute("name");
    } else {
      property = context.getStringAttribute("property");
    }
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String nestedSelect = context.getStringAttribute("select");
    String nestedResultMap = context.getStringAttribute("resultMap",
        processNestedResultMappings(context, Collections.emptyList(), resultType));
    String notNullColumn = context.getStringAttribute("notNullColumn");
    String columnPrefix = context.getStringAttribute("columnPrefix");
    String typeHandler = context.getStringAttribute("typeHandler");
    String resultSet = context.getStringAttribute("resultSet");
    String foreignColumn = context.getStringAttribute("foreignColumn");
    boolean lazy = "lazy".equals(context.getStringAttribute("fetchType", configuration.isLazyLoadingEnabled() ? "lazy" : "eager"));
    Class<?> javaTypeClass = resolveClass(javaType);
    Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
    return builderAssistant.buildResultMapping(resultType, property, column, javaTypeClass, jdbcTypeEnum, nestedSelect, nestedResultMap, notNullColumn, columnPrefix, typeHandlerClass, flags, resultSet, foreignColumn, lazy);
  }

  private String processNestedResultMappings(XNode context, List<ResultMapping> resultMappings, Class<?> enclosingType) throws Exception {
    if ("association".equals(context.getName())
        || "collection".equals(context.getName())
        || "case".equals(context.getName())) {
      if (context.getStringAttribute("select") == null) {
        validateCollection(context, enclosingType);
        // resultMapElement 解析 resultMap 入口方法
        ResultMap resultMap = resultMapElement(context, resultMappings, enclosingType);
        // 返回 resultMap id
        return resultMap.getId();
      }
    }
    return null;
  }

  protected void validateCollection(XNode context, Class<?> enclosingType) {
    if ("collection".equals(context.getName()) && context.getStringAttribute("resultMap") == null
        && context.getStringAttribute("javaType") == null) {
      MetaClass metaResultType = MetaClass.forClass(enclosingType, configuration.getReflectorFactory());
      String property = context.getStringAttribute("property");
      if (!metaResultType.hasSetter(property)) {
        throw new BuilderException(
          "Ambiguous collection type for property '" + property + "'. You must specify 'javaType' or 'resultMap'.");
      }
    }
  }

  /**
   * Mapper 接口绑定流程
   */
  private void bindMapperForNamespace() {
    String namespace = builderAssistant.getCurrentNamespace();
    if (namespace != null) {
      Class<?> boundType = null;
      try {
        // 根据命名空间解析 mapper 类型
        boundType = Resources.classForName(namespace);
      } catch (ClassNotFoundException e) {
        //ignore, bound type is not required
      }
      if (boundType != null) {
        // 检测当前 mapper 类是否被绑定过
        if (!configuration.hasMapper(boundType)) {
          // Spring may not know the real resource name so we set a flag
          // to prevent loading again this resource from the mapper interface
          // look at MapperAnnotationBuilder#loadXmlResource
          configuration.addLoadedResource("namespace:" + namespace);
          configuration.addMapper(boundType);
        }
      }
    }
  }

}
