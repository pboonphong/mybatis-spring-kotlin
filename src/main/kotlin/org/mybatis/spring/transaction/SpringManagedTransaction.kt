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
package org.mybatis.spring.transaction

import org.apache.ibatis.transaction.Transaction
import org.mybatis.logging.LoggerFactory
import org.springframework.jdbc.datasource.ConnectionHolder
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

/**
 * `SpringManagedTransaction` handles the lifecycle of a JDBC connection.
 * It retrieves a connection from Spring's transaction manager and returns it back to it
 * when it is no longer needed.
 *
 *
 * If Spring's transaction handling is active it will no-op all commit/rollback/close calls
 * assuming that the Spring transaction manager will do the job.
 *
 *
 * If it is not it will behave like `JdbcTransaction`.

 * @author Hunter Presnall
 * *
 * @author Eduardo Macarron
 * *
 * *
 * @version $Id$
 */
class SpringManagedTransaction(private val dataSource: DataSource) : Transaction {

  private var connection: Connection? = null

  private var isConnectionTransactional: Boolean = false

  private var autoCommit: Boolean = false

  /**
   * {@inheritDoc}
   */
  @Throws(SQLException::class)
  override fun getConnection(): Connection? {
    if (this.connection == null) {
      openConnection()
    }
    return this.connection
  }

  /**
   * Gets a connection from Spring transaction manager and discovers if this
   * `Transaction` should manage connection or let it to Spring.
   *
   *
   * It also reads autocommit setting because when using Spring Transaction MyBatis
   * thinks that autocommit is always false and will always call commit/rollback
   * so we need to no-op that calls.
   */
  @Throws(SQLException::class)
  private fun openConnection() {
    this.connection = DataSourceUtils.getConnection(this.dataSource)
    this.autoCommit = this.connection!!.autoCommit
    this.isConnectionTransactional = DataSourceUtils.isConnectionTransactional(this.connection!!, this.dataSource)

    logger.debug {
        "JDBC Connection [$connection] will" + (if (this.isConnectionTransactional) " " else " not ") + "be managed by Spring"}
  }

  /**
   * {@inheritDoc}
   */
  @Throws(SQLException::class)
  override fun commit() {
    if (this.connection != null && !this.isConnectionTransactional && !this.autoCommit) {
      logger.debug {"Committing JDBC Connection [${this.connection}]"}
      this.connection!!.commit()
    }
  }

  /**
   * {@inheritDoc}
   */
  @Throws(SQLException::class)
  override fun rollback() {
    if (this.connection != null && !this.isConnectionTransactional && !this.autoCommit) {
      logger.debug {"Rolling back JDBC Connection [$connection]"}
      this.connection!!.rollback()
    }
  }

  /**
   * {@inheritDoc}
   */
  @Throws(SQLException::class)
  override fun close() {
    DataSourceUtils.releaseConnection(this.connection, this.dataSource)
  }

  /**
   * {@inheritDoc}
   */
  @Throws(SQLException::class)
  override fun getTimeout(): Int? {
    val holder = TransactionSynchronizationManager.getResource(dataSource) as ConnectionHolder?
    if (holder != null && holder.hasTimeout()) {
      return holder.timeToLiveInSeconds
    }
    return null
  }

  companion object {

    private val logger = LoggerFactory.getLogger(SpringManagedTransaction::class.java)
  }

}
