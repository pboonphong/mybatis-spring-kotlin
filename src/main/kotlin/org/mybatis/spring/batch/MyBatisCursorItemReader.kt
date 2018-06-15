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

import org.springframework.util.ClassUtils.getShortName

import java.util.HashMap

import org.apache.ibatis.cursor.Cursor
import org.apache.ibatis.session.ExecutorType
import org.apache.ibatis.session.SqlSession
import org.apache.ibatis.session.SqlSessionFactory
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader

/**
 * @author Guillaume Darmont / guillaume@dropinocean.com
 */
class MyBatisCursorItemReader<T>(private var sqlSessionFactory: SqlSessionFactory, private var queryId: String) : AbstractItemCountingItemStreamItemReader<T>() {

  private var sqlSession: SqlSession? = null

  private var parameterValues: Map<String, Any>? = null

  private var cursor: Cursor<T>? = null
  private var cursorIterator: Iterator<T>? = null

  init {
    setName(getShortName(MyBatisCursorItemReader::class.java))
  }

  /**
   * The parameter values to be used for the query execution.
   *
   * @param parameterValues the values keyed by the parameter named used in the query string.
   */
  fun setParameterValues(parameterValues: Map<String, Any>) {
    this.parameterValues = parameterValues
  }

  @Throws(Exception::class)
  override fun doRead(): T? {
    var next: T? = null
    if (cursorIterator!!.hasNext()) {
      next = cursorIterator!!.next()
    }
    return next
  }

  @Throws(Exception::class)
  override fun doOpen() {
    val parameters = HashMap<String, Any>()
    if (parameterValues != null) {
      parameters.putAll(parameterValues!!)
    }

    sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE)
    cursor = sqlSession!!.selectCursor<T>(queryId, parameters)
    cursorIterator = cursor!!.iterator()
  }

  @Throws(Exception::class)
  override fun doClose() {
    cursor!!.close()
    sqlSession!!.close()
    cursorIterator = null
  }

}
