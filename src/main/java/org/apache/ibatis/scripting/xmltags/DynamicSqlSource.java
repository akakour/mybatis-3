/**
 *    Copyright 2009-2021 the original author or authors.
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
package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 */
public class DynamicSqlSource implements SqlSource {

  private final Configuration configuration;
  private final SqlNode rootSqlNode;

  public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
    this.configuration = configuration;
    this.rootSqlNode = rootSqlNode;
  }

  /**
   * 从sqlsource到生成boudsql对象
   *
   * @param parameterObject
   * @return
   */
  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    // context的sqlBuilder属性是用来拼接sql文的
    DynamicContext context = new DynamicContext(configuration, parameterObject);
    /**
     * 核心 烧脑的一句
     * 会反复根据sqlsource树来调用各个sqlnode的apply方法
     * 比如 trimsqlNode，ifsqlnode等
     * 说到底一句话就是根据之前解析生成的sqlsource树反复的调用sqlnode的apply处理，将合适的结果拼接到context的sqlBuilder中
     */
    rootSqlNode.apply(context);
    SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
    Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
    // context.getSql()：带#{}的sql文。生成的Sqlsource是静态的StaticSqlSource，其中sql文就是带？的
    // 这一步完成了从早期解析的动态sqlsource（树）到最终该次查询的静态sql文。
    SqlSource sqlSource = sqlSourceParser.parse(context.getSql(), parameterType, context.getBindings());
    // 封装？和入参的映射
    BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
    context.getBindings().forEach(boundSql::setAdditionalParameter);
    return boundSql;
  }

}
