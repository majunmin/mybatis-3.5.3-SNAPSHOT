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
package org.apache.ibatis.plugin;

import java.util.Properties;

/**
 * 默认情况下， MyBatis 允许拦截器拦截
 * Executor 的方法、 ParameterHandler 的方法、 ResultSetHandler 的方法 以及 StatementHandler的方法
 *  - Executor中的update()、query() 、flushStatements()、commit()、rollback()、 getTransaction()、 close()、 isClosed()。
 *  - ParameterHandler 中的 getParameterObject()、 setParameters()。
 *  - ResultSetHandler 中的 handleResultSets()、 handleOu飞putParameters()。
 *  - StatementHandler 中的 prepare()、 parameterize() 、 batch()、 update() 、 query()。
 * @author Clinton Begin
 */
public interface Interceptor {

  /**
   * 执行拦截逻辑
   * @param invocation
   * @return
   * @throws Throwable
   */
  Object intercept(Invocation invocation) throws Throwable;

  /**
   * 决定是否触发 intercept()
   * @param target
   * @return
   */
  default Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  /**
   * 根据配置初始化 Interceptor 象
   * @param properties
   */
  default void setProperties(Properties properties) {
    // NOP
  }

}
