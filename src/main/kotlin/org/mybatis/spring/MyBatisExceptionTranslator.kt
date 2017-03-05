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
package org.mybatis.spring

import java.sql.SQLException

import javax.sql.DataSource

import org.apache.ibatis.exceptions.PersistenceException
import org.springframework.dao.DataAccessException
import org.springframework.dao.support.PersistenceExceptionTranslator
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
import org.springframework.jdbc.support.SQLExceptionTranslator
import org.springframework.transaction.TransactionException

/**
 * Default exception translator.
 *
 * Translates MyBatis SqlSession returned exception into a Spring
 * `DataAccessException` using Spring's `SQLExceptionTranslator`
 * Can load `SQLExceptionTranslator` eagerly of when the
 * first exception is translated.
 *
 * @author Eduardo Macarron
 *
 * @version $Id$
 */
class MyBatisExceptionTranslator
/**
 * Creates a new `DataAccessExceptionTranslator` instance.
 *
 * @param dataSource DataSource to use to find metadata and establish which error codes are usable.
 *
 * @param exceptionTranslatorLazyInit if true, the translator instantiates internal stuff only the first time will
 * *        have the need to translate exceptions.
 */
(private val dataSource: DataSource, exceptionTranslatorLazyInit: Boolean) : PersistenceExceptionTranslator {

  private var exceptionTranslator: SQLExceptionTranslator? = null

  init {
    if (!exceptionTranslatorLazyInit) {
      this.initExceptionTranslator()
    }
  }

  /**
   * {@inheritDoc}
   */
  override fun translateExceptionIfPossible(e: RuntimeException): DataAccessException? {
    var e = e
    if (e is PersistenceException) {
      // Batch exceptions come inside another PersistenceException
      // recursion has a risk of infinite loop so better make another if
      if (e.cause is PersistenceException) {
        e = e.cause as PersistenceException
      }
      if (e.cause is SQLException) {
        this.initExceptionTranslator()
        return this.exceptionTranslator!!.translate(e.message + "\n", null, e.cause as SQLException)
      } else if (e.cause is TransactionException) {
        throw e.cause as TransactionException
      }
      return MyBatisSystemException(e)
    }
    return null
  }

  /**
   * Initializes the internal translator reference.
   */
  @Synchronized private fun initExceptionTranslator() {
    if (this.exceptionTranslator == null) {
      this.exceptionTranslator = SQLErrorCodeSQLExceptionTranslator(this.dataSource)
    }
  }
}
