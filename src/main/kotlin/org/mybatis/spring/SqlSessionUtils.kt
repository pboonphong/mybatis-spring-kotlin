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

import org.apache.ibatis.exceptions.PersistenceException
import org.mybatis.logging.LoggerFactory
import org.apache.ibatis.session.ExecutorType
import org.apache.ibatis.session.SqlSession
import org.apache.ibatis.session.SqlSessionFactory
import org.mybatis.spring.transaction.SpringManagedTransactionFactory
import org.springframework.dao.TransientDataAccessResourceException
import org.springframework.dao.support.PersistenceExceptionTranslator
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.transaction.support.TransactionSynchronizationAdapter
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Handles MyBatis SqlSession life cycle. It can register and get SqlSessions from
 * Spring `TransactionSynchronizationManager`. Also works if no transaction is active.
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 *
 * @version $Id$
 */
class SqlSessionUtils {

  /**
   * This class can't be instantiated, exposes static utility methods only.
   */
  companion object {

    private val logger = LoggerFactory.getLogger(SqlSessionUtils::class.java)

    /**
     * Creates a new MyBatis `SqlSession` from the `SqlSessionFactory`
     * provided as a parameter and using its `DataSource` and `ExecutorType`
     *
     * @param sessionFactory a MyBatis `SqlSessionFactory` to create new sessions
     * @return a MyBatis `SqlSession`
     * @throws TransientDataAccessResourceException if a transaction is active and the
     *             `SqlSessionFactory` is not using a `SpringManagedTransactionFactory`
     */
    @JvmStatic
    fun getSqlSession(sessionFactory: SqlSessionFactory): SqlSession {
      val executorType = sessionFactory.configuration.defaultExecutorType
      return getSqlSession(sessionFactory, executorType, null)
    }

    /**
     * Gets an SqlSession from Spring Transaction Manager or creates a new one if needed.
     * Tries to get a SqlSession out of current transaction. If there is not any, it creates a new one.
     * Then, it synchronizes the SqlSession with the transaction if Spring TX is active and
     * `SpringManagedTransactionFactory` is configured as a transaction manager.
     *
     * @param sessionFactory a MyBatis `SqlSessionFactory` to create new sessions
     * @param executorType The executor type of the SqlSession to create
     * @param exceptionTranslator Optional. Translates SqlSession.commit() exceptions to Spring exceptions.
     * @throws TransientDataAccessResourceException if a transaction is active and the
     *             `SqlSessionFactory` is not using a `SpringManagedTransactionFactory`
     * @see SpringManagedTransactionFactory
     */
    @JvmStatic
    fun getSqlSession(sessionFactory: SqlSessionFactory, executorType: ExecutorType, exceptionTranslator: PersistenceExceptionTranslator?): SqlSession {

      val holder = TransactionSynchronizationManager.getResource(sessionFactory) as? SqlSessionHolder

      var session: SqlSession? = sessionHolder(executorType, holder)
      if (session != null) {
        return session
      }

      logger.debug {"Creating a new SqlSession"}
      session = sessionFactory.openSession(executorType)

      registerSessionHolder(sessionFactory, executorType, exceptionTranslator, session)

      return session
    }

    /**
     * Register session holder if synchronization is active (i.e. a Spring TX is active).
     *
     * Note: The DataSource used by the Environment should be synchronized with the
     * transaction either through DataSourceTxMgr or another tx synchronization.
     * Further assume that if an exception is thrown, whatever started the transaction will
     * handle closing / rolling back the Connection associated with the SqlSession.
     *
     * @param sessionFactory sqlSessionFactory used for registration.
     * @param executorType executorType used for registration.
     * @param exceptionTranslator persistenceExceptionTranslater used for registration.
     * @param session sqlSessionTemplate used for registration.
     */
    private fun registerSessionHolder(sessionFactory: SqlSessionFactory, executorType: ExecutorType,
                                      exceptionTranslator: PersistenceExceptionTranslator?, session: SqlSession) {
      val holder: SqlSessionHolder
      if (TransactionSynchronizationManager.isSynchronizationActive()) {
        val environment = sessionFactory.configuration.environment

        if (environment.transactionFactory is SpringManagedTransactionFactory) {
          logger.debug {"Registering transaction synchronization for SqlSession [$session]"}

          holder = SqlSessionHolder(session, executorType, exceptionTranslator)
          TransactionSynchronizationManager.bindResource(sessionFactory, holder)
          TransactionSynchronizationManager.registerSynchronization(SqlSessionSynchronization(holder, sessionFactory))
          holder.isSynchronizedWithTransaction = true
          holder.requested()
        } else {
          if (TransactionSynchronizationManager.getResource(environment.dataSource) == null) {
            logger.debug {"SqlSession [$session] was not registered for synchronization because DataSource is not transactional"}
          } else {
            throw TransientDataAccessResourceException(
                "SqlSessionFactory must be using a SpringManagedTransactionFactory in order to use Spring transaction synchronization")
          }
        }
      } else {
        logger.debug {"SqlSession [$session] was not registered for synchronization because synchronization is not active"}
      }
    }

    private fun sessionHolder(executorType: ExecutorType, holder: SqlSessionHolder?): SqlSession? {
      var session: SqlSession? = null
      if (holder != null && holder.isSynchronizedWithTransaction) {
        if (holder.executorType !== executorType) {
          throw TransientDataAccessResourceException("Cannot change the ExecutorType when there is an existing transaction")
        }

        holder.requested()

        logger.debug {"Fetched SqlSession [${holder.sqlSession}] from current transaction"}
        session = holder.sqlSession
      }
      return session
    }

    /**
     * Checks if `SqlSession` passed as an argument is managed by Spring `TransactionSynchronizationManager`
     * If it is not, it closes it, otherwise it just updates the reference counter and
     * lets Spring call the close callback when the managed transaction ends
     *
     * @param session
     * @param sessionFactory
     */
    @JvmStatic
    fun closeSqlSession(session: SqlSession, sessionFactory: SqlSessionFactory) {
      val holder = TransactionSynchronizationManager.getResource(sessionFactory) as SqlSessionHolder?
      if (holder != null && holder.sqlSession === session) {
        logger.debug {"Releasing transactional SqlSession [$session]"}
        holder.released()
      } else {
        logger.debug {"Closing non transactional SqlSession [$session]"}
        session.close()
      }
    }

    /**
     * Returns if the `SqlSession` passed as an argument is being managed by Spring
     *
     * @param session a MyBatis SqlSession to check
     * @param sessionFactory the SqlSessionFactory which the SqlSession was built with
     * @return true if session is transactional, otherwise false
     */
    @JvmStatic
    fun isSqlSessionTransactional(session: SqlSession, sessionFactory: SqlSessionFactory): Boolean {
      val holder = TransactionSynchronizationManager.getResource(sessionFactory) as SqlSessionHolder?

      return holder != null && holder.sqlSession === session
    }

    /**
     * Callback for cleaning up resources. It cleans TransactionSynchronizationManager and
     * also commits and closes the `SqlSession`.
     * It assumes that `Connection` life cycle will be managed by
     * `DataSourceTransactionManager` or `JtaTransactionManager`
     */
    private class SqlSessionSynchronization(private val holder: SqlSessionHolder, private val sessionFactory: SqlSessionFactory) : TransactionSynchronizationAdapter() {

      private var holderActive = true

      /**
       * {@inheritDoc}
       */
      override fun getOrder(): Int {
        // order right before any Connection synchronization
        return DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 1
      }

      /**
       * {@inheritDoc}
       */
      override fun suspend() {
        if (this.holderActive) {
          logger.debug {"Transaction synchronization suspending SqlSession [${this.holder.sqlSession}]"}
          TransactionSynchronizationManager.unbindResource(this.sessionFactory)
        }
      }

      /**
       * {@inheritDoc}
       */
      override fun resume() {
        if (this.holderActive) {
          logger.debug {"Transaction synchronization resuming SqlSession [${this.holder.sqlSession}]"}
          TransactionSynchronizationManager.bindResource(this.sessionFactory, this.holder)
        }
      }

      /**
       * {@inheritDoc}
       */
      override fun beforeCommit(readOnly: Boolean) {
        // Connection commit or rollback will be handled by ConnectionSynchronization or
        // DataSourceTransactionManager.
        // But, do cleanup the SqlSession / Executor, including flushing BATCH statements so
        // they are actually executed.
        // SpringManagedTransaction will no-op the commit over the jdbc connection
        // TODO This updates 2nd level caches but the tx may be rolledback later on!
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
          try {
            logger.debug {"Transaction synchronization committing SqlSession [${this.holder.sqlSession}]"}
            this.holder.sqlSession.commit()
          } catch (p: PersistenceException) {
            if (this.holder.persistenceExceptionTranslator != null) {
              val translated = this.holder.persistenceExceptionTranslator.translateExceptionIfPossible(p)
              if (translated != null) {
                throw translated
              }
            }
            throw p
          }
        }
      }

      /**
       * {@inheritDoc}
       */
      override fun beforeCompletion() {
        // Issue #18 Close SqlSession and deregister it now
        // because afterCompletion may be called from a different thread
        if (!this.holder.isOpen) {
          logger.debug {"Transaction synchronization deregistering SqlSession [${this.holder.sqlSession}]"}
          TransactionSynchronizationManager.unbindResource(sessionFactory)
          this.holderActive = false
          logger.debug {"Transaction synchronization closing SqlSession [${this.holder.sqlSession}]"}
          this.holder.sqlSession.close()
        }
      }

      /**
       * {@inheritDoc}
       */
      override fun afterCompletion(status: Int) {
        if (this.holderActive) {
          // afterCompletion may have been called from a different thread
          // so avoid failing if there is nothing in this one
          logger.debug {"Transaction synchronization deregistering SqlSession [${this.holder.sqlSession}]"}
          TransactionSynchronizationManager.unbindResourceIfPossible(sessionFactory)
          this.holderActive = false
          logger.debug {"Transaction synchronization closing SqlSession [${this.holder.sqlSession}]"}
          this.holder.sqlSession.close()
        }
        this.holder.reset()
      }
    }

  }
}
