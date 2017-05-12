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

import org.springframework.util.Assert.isTrue
import org.springframework.util.Assert.notNull

import org.mybatis.logging.LoggerFactory
import org.apache.ibatis.session.ExecutorType
import org.apache.ibatis.session.SqlSessionFactory
import org.mybatis.spring.SqlSessionTemplate
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.InitializingBean
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.dao.InvalidDataAccessResourceUsageException

/**
 * `ItemWriter` that uses the batching features from
 * `SqlSessionTemplate` to execute a batch of statements for all items
 * provided.
 *
 * Provided to facilitate the migration from Spring-Batch iBATIS 2 writers to MyBatis 3
 *
 * The user must provide a MyBatis statement id that points to the SQL statement defined
 * in the MyBatis.
 *
 * It is expected that [.write] is called inside a transaction. If it is not
 * each statement call will be autocommitted and flushStatements will return no results.
 *
 * The writer is thread safe after its properties are set (normal singleton
 * behavior), so it can be used to write in multiple concurrent transactions.
 *
 * @author Eduardo Macarron
 * @since 1.1.0
 * @version $Id$
 */
class MyBatisBatchItemWriter<T> : ItemWriter<T>, InitializingBean {

  /**
   * Public setter for the [SqlSessionTemplate].
   *
   * @param sqlSessionTemplate the SqlSessionTemplate
   */
  var sqlSessionTemplate: SqlSessionTemplate? = null

  /**
   * Public setter for the statement id identifying the statement in the SqlMap
   * configuration file.
   *
   * @param statementId the id for the statement
   */
  var statementId: String? = null

  /**
   * Public setter for the flag that determines whether an assertion is made
   * that all items cause at least one row to be updated.
   *
   * @param assertUpdates the flag to set. Defaults to true;
   */
  var assertUpdates = true

  /**
   * Public setter for [SqlSessionFactory] for injection purposes.
   *
   * @param sqlSessionFactory SqlSessionFactory
   */
  fun setSqlSessionFactory(sqlSessionFactory: SqlSessionFactory) {
    if (sqlSessionTemplate == null) {
      this.sqlSessionTemplate = SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH)
    }
  }

  /**
   * Check mandatory properties - there must be an SqlSession and a statementId.
   */
  override fun afterPropertiesSet() {
    notNull(sqlSessionTemplate, "A SqlSessionFactory or a SqlSessionTemplate is required.")
    isTrue(ExecutorType.BATCH == sqlSessionTemplate!!.executorType, "SqlSessionTemplate's executor type must be BATCH")
    notNull(statementId, "A statementId is required.")
  }

  /**
   * {@inheritDoc}
   */
  override fun write(items: List<T>) {

    if (!items.isEmpty()) {

      logger.debug {"Executing batch with ${items.size} items."}

      for (item in items) {
        sqlSessionTemplate!!.update(statementId!!, item!!)
      }

      val results = sqlSessionTemplate!!.flushStatements()

      if (assertUpdates) {
        if (results!!.size != 1) {
          throw InvalidDataAccessResourceUsageException("Batch execution returned invalid results. " +
              "Expected 1 but number of BatchResult objects returned was ${results.size}")
        }

        val updateCounts = results[0].updateCounts

        for (i in updateCounts.indices) {
          val value = updateCounts[i]
          if (value == 0) {
            throw EmptyResultDataAccessException("Item $i of ${updateCounts.size} did not update any rows: [${items[i]}]", 1)
          }
        }
      }
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(MyBatisBatchItemWriter::class.java)
  }

}
