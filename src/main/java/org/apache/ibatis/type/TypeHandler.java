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
package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 在向数据库存储或读取数据时，我们需要将数据库字段类型和 Java 类型进行一个转换。
 * 比如数据库中有 CHAR 和 VARCHAR 等类型，但 Java 中没有这些类型，不过 Java 有 String 类型。
 * 所以我们在从数据库中读取 CHAR 和 VARCHAR 类型的数据时，就可以把它们转成 String 。
 * 在 MyBatis 中，数据库类型和 Java 类型之间的转换任务是委托给类型处理器 TypeHandler 去处理的。
 * @author Clinton Begin
 */
public interface TypeHandler<T> {

  void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;

  /**
   * @param columnName Colunm name, when configuration <code>useColumnLabel</code> is <code>false</code>
   */
  T getResult(ResultSet rs, String columnName) throws SQLException;

  T getResult(ResultSet rs, int columnIndex) throws SQLException;

  T getResult(CallableStatement cs, int columnIndex) throws SQLException;

}
