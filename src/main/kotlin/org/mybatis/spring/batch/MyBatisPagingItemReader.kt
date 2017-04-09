/**
 *    Copyright 2010-2017 the original author or authors.
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
package org.mybatis.spring.batch

import org.springframework.util.Assert.notNull
import org.springframework.util.ClassUtils.getShortName

import java.util.HashMap
import java.util.concurrent.CopyOnWriteArrayList

import org.apache.ibatis.session.ExecutorType
import org.apache.ibatis.session.SqlSessionFactory
import org.mybatis.spring.SqlSessionTemplate
import org.springframework.batch.item.database.AbstractPagingItemReader

/**
 * `org.springframework.batch.item.ItemReader` for reading database  records using MyBatis in a paging fashion.
 * Provided to facilitate the migration from Spring-Batch iBATIS 2 page item readers to MyBatis 3.
 *
 * @author Eduardo Macarron
 * @since 1.1.0
 * @version $Id$
 */
class MyBatisPagingItemReader<T> : AbstractPagingItemReader<T>() {

  private var queryId: String? = null

  private var sqlSessionFactory: SqlSessionFactory? = null

  private var sqlSessionTemplate: SqlSessionTemplate? = null

  private var parameterValues: Map<String, Any>? = null

  init {
    setName(getShortName(MyBatisPagingItemReader::class.java))
  }

  /**
   * Public setter for [SqlSessionFactory] for injection purposes.
   *
   * @param sqlSessionFactory SqlSessionFactory
   */
  fun setSqlSessionFactory(sqlSessionFactory: SqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory
  }

  /**
   * Public setter for the statement id identifying the statement in the SqlMap
   * configuration file.
   *
   * @param queryId the id for the statement
   */
  fun setQueryId(queryId: String) {
    this.queryId = queryId
  }

  /**
   * The parameter values to be used for the query execution.
   *
   * @param parameterValues the values keyed by the parameter named used in the query string.
   */
  fun setParameterValues(parameterValues: Map<String, Any>) {
    this.parameterValues = parameterValues
  }

  /**
   * Check mandatory properties.
   * @see org.springframework.beans.factory.InitializingBean.afterPropertiesSet
   */
  @Throws(Exception::class)
  override fun afterPropertiesSet() {
    super.afterPropertiesSet()
    notNull(sqlSessionFactory, "sqlSessionFactory must not be null!")
    sqlSessionTemplate = SqlSessionTemplate(sqlSessionFactory!!, ExecutorType.BATCH)
    notNull(queryId, "queryId must not be null!")
  }

  override fun doReadPage() {
    val parameters = HashMap<String, Any>()
    if (parameterValues != null) {
      parameters.putAll(parameterValues!!)
    }
    parameters.put("_page", page)
    parameters.put("_pagesize", pageSize)
    parameters.put("_skiprows", page * pageSize)
    if (results == null) {
      results = CopyOnWriteArrayList<T>()
    } else {
      results.clear()
    }
    results.addAll(sqlSessionTemplate!!.selectList<T>(queryId!!, parameters)!!)
  }

  override fun doJumpToPage(itemIndex: Int) {
    // Not Implemented
  }

}
