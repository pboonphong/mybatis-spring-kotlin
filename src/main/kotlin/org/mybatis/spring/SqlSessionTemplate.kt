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

import java.lang.reflect.Proxy.newProxyInstance
import org.apache.ibatis.reflection.ExceptionUtil.unwrapThrowable
import org.mybatis.spring.SqlSessionUtils.Companion.closeSqlSession
import org.mybatis.spring.SqlSessionUtils.Companion.getSqlSession
import org.mybatis.spring.SqlSessionUtils.Companion.isSqlSessionTransactional

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.sql.Connection

import org.apache.ibatis.cursor.Cursor
import org.apache.ibatis.exceptions.PersistenceException
import org.apache.ibatis.executor.BatchResult
import org.apache.ibatis.session.Configuration
import org.apache.ibatis.session.ExecutorType
import org.apache.ibatis.session.ResultHandler
import org.apache.ibatis.session.RowBounds
import org.apache.ibatis.session.SqlSession
import org.apache.ibatis.session.SqlSessionFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.dao.support.PersistenceExceptionTranslator

/**
 * Thread safe, Spring managed, `SqlSession` that works with Spring
 * transaction management to ensure that that the actual SqlSession used is the
 * one associated with the current Spring transaction. In addition, it manages
 * the session life-cycle, including closing, committing or rolling back the
 * session as necessary based on the Spring transaction configuration.
 *
 *
 * The template needs a SqlSessionFactory to create SqlSessions, passed as a
 * constructor argument. It also can be constructed indicating the executor type
 * to be used, if not, the default executor type, defined in the session factory
 * will be used.
 *
 *
 * This template converts MyBatis PersistenceExceptions into unchecked
 * DataAccessExceptions, using, by default, a `MyBatisExceptionTranslator`.
 *
 *
 * Because SqlSessionTemplate is thread safe, a single instance can be shared
 * by all DAOs; there should also be a small memory savings by doing this. This
 * pattern can be used in Spring configuration files as follows:
 *
 *
 * `&lt;bean id=&quot;sqlSessionTemplate&quot; class=&quot;org.mybatis.spring.SqlSessionTemplate&quot;&gt;
 * &lt;constructor-arg ref=&quot;sqlSessionFactory&quot; /&gt;
 * &lt;/bean&gt;`
 *
 *
 * @author Putthiphong Boonphong
 * @author Hunter Presnall
 * @author Eduardo Macarron
 *
 * @see SqlSessionFactory
 * @see MyBatisExceptionTranslator
 *
 * @version $Id$
 */
class SqlSessionTemplate
/**
 * Constructs a Spring managed `SqlSession` with the given
 * `SqlSessionFactory` and `ExecutorType`.
 * A custom `SQLExceptionTranslator` can be provided as an
 * argument so any `PersistenceException` thrown by MyBatis
 * can be custom translated to a `RuntimeException`
 * The `SQLExceptionTranslator` can also be null and thus no
 * exception translation will be done and MyBatis exceptions will be
 * thrown
 *
 * @param sqlSessionFactory
 * @param executorType
 * @param exceptionTranslator
 */
@JvmOverloads constructor(val sqlSessionFactory: SqlSessionFactory,
                          val executorType: ExecutorType = sqlSessionFactory.configuration.defaultExecutorType,
                          val exceptionTranslator: PersistenceExceptionTranslator? = MyBatisExceptionTranslator(
                              sqlSessionFactory.configuration.environment.dataSource, true)) : SqlSession, DisposableBean {

  private val sqlSessionProxy: SqlSession

  init {
    this.sqlSessionProxy = newProxyInstance(
        SqlSessionFactory::class.java.classLoader,
        arrayOf(SqlSession::class.java),
        SqlSessionInterceptor()) as SqlSession
  }

  /**
   * {@inheritDoc}
   */
  override fun <T> selectOne(statement: String): T? {
    return this.sqlSessionProxy.selectOne<T>(statement)
  }

  /**
   * {@inheritDoc}
   */
  override fun <T> selectOne(statement: String, parameter: Any?): T? {
    return this.sqlSessionProxy.selectOne<T>(statement, parameter)
  }

  /**
   * {@inheritDoc}
   */
  override fun <K, V> selectMap(statement: String, mapKey: String): MutableMap<K, V>? {
    return this.sqlSessionProxy.selectMap<K, V>(statement, mapKey)
  }

  /**
   * {@inheritDoc}
   */
  override fun <K, V> selectMap(statement: String, parameter: Any?, mapKey: String): MutableMap<K, V>? {
    return this.sqlSessionProxy.selectMap<K, V>(statement, parameter, mapKey)
  }

  /**
   * {@inheritDoc}
   */
  override fun <K, V> selectMap(statement: String, parameter: Any?, mapKey: String, rowBounds: RowBounds): MutableMap<K, V>? {
    return this.sqlSessionProxy.selectMap<K, V>(statement, parameter, mapKey, rowBounds)
  }

  /**
   * {@inheritDoc}
   */
  override fun <T> selectCursor(statement: String): Cursor<T>? {
    return this.sqlSessionProxy.selectCursor<T>(statement)
  }

  /**
   * {@inheritDoc}
   */
  override fun <T> selectCursor(statement: String, parameter: Any?): Cursor<T>? {
    return this.sqlSessionProxy.selectCursor<T>(statement, parameter)
  }

  /**
   * {@inheritDoc}
   */
  override fun <T> selectCursor(statement: String, parameter: Any?, rowBounds: RowBounds): Cursor<T>? {
    return this.sqlSessionProxy.selectCursor<T>(statement, parameter, rowBounds)
  }

  /**
   * {@inheritDoc}
   */
  override fun <E> selectList(statement: String): MutableList<E>? {
    return this.sqlSessionProxy.selectList<E>(statement)
  }

  /**
   * {@inheritDoc}
   */
  override fun <E> selectList(statement: String, parameter: Any?): MutableList<E>? {
    return this.sqlSessionProxy.selectList<E>(statement, parameter)
  }

  /**
   * {@inheritDoc}
   */
  override fun <E> selectList(statement: String, parameter: Any?, rowBounds: RowBounds): MutableList<E>? {
    return this.sqlSessionProxy.selectList<E>(statement, parameter, rowBounds)
  }

  /**
   * {@inheritDoc}
   */
  override fun select(statement: String, handler: ResultHandler<Any>) {
    this.sqlSessionProxy.select(statement, handler)
  }

  /**
   * {@inheritDoc}
   */
  override fun select(statement: String, parameter: Any?, handler: ResultHandler<Any>) {
    this.sqlSessionProxy.select(statement, parameter, handler)
  }

  /**
   * {@inheritDoc}
   */
  override fun select(statement: String, parameter: Any?, rowBounds: RowBounds, handler: ResultHandler<Any>) {
    this.sqlSessionProxy.select(statement, parameter, rowBounds, handler)
  }

  /**
   * {@inheritDoc}
   */
  override fun insert(statement: String): Int {
    return this.sqlSessionProxy.insert(statement)
  }

  /**
   * {@inheritDoc}
   */
  override fun insert(statement: String, parameter: Any?): Int {
    return this.sqlSessionProxy.insert(statement, parameter)
  }

  /**
   * {@inheritDoc}
   */
  override fun update(statement: String): Int {
    return this.sqlSessionProxy.update(statement)
  }

  /**
   * {@inheritDoc}
   */
  override fun update(statement: String, parameter: Any?): Int {
    return this.sqlSessionProxy.update(statement, parameter)
  }

  /**
   * {@inheritDoc}
   */
  override fun delete(statement: String): Int {
    return this.sqlSessionProxy.delete(statement)
  }

  /**
   * {@inheritDoc}
   */
  override fun delete(statement: String, parameter: Any?): Int {
    return this.sqlSessionProxy.delete(statement, parameter)
  }

  /**
   * {@inheritDoc}
   */
  override fun <T> getMapper(type: Class<T>): T {
    return configuration.getMapper(type, this)
  }

  /**
   * {@inheritDoc}
   */
  override fun commit() {
    throw UnsupportedOperationException("Manual commit is not allowed over a Spring managed SqlSession")
  }

  /**
   * {@inheritDoc}
   */
  override fun commit(force: Boolean) {
    throw UnsupportedOperationException("Manual commit is not allowed over a Spring managed SqlSession")
  }

  /**
   * {@inheritDoc}
   */
  override fun rollback() {
    throw UnsupportedOperationException("Manual rollback is not allowed over a Spring managed SqlSession")
  }

  /**
   * {@inheritDoc}
   */
  override fun rollback(force: Boolean) {
    throw UnsupportedOperationException("Manual rollback is not allowed over a Spring managed SqlSession")
  }

  /**
   * {@inheritDoc}
   */
  override fun close() {
    throw UnsupportedOperationException("Manual close is not allowed over a Spring managed SqlSession")
  }

  /**
   * {@inheritDoc}
   */
  override fun clearCache() {
    this.sqlSessionProxy.clearCache()
  }

  /**
   * {@inheritDoc}
   */
  override fun getConfiguration(): Configuration {
    return this.sqlSessionFactory.configuration
  }

  /**
   * {@inheritDoc}
   */
  override fun getConnection(): Connection {
    println(this.sqlSessionProxy.connection != null)
    return this.sqlSessionProxy.connection
  }

  /**
   * {@inheritDoc}
   * @since 1.0.2
   */
  override fun flushStatements(): MutableList<BatchResult>? {
    return this.sqlSessionProxy.flushStatements()
  }

  /**
   * Allow gently dispose bean:
   *
   * `&lt;bean id=&quot;sqlSession&quot; class=&quot;org.mybatis.spring.SqlSessionTemplate&quot;&gt;
   * &lt;constructor-arg index=&quot;0&quot; ref=&quot;sqlSessionFactory&quot; /&gt;
   * &lt;/bean&gt;`
   *
   * The implementation of [DisposableBean] forces spring context to use [DisposableBean.destroy] method instead of [SqlSessionTemplate.close] to shutdown gently.
   *
   * @see SqlSessionTemplate.close
   * @see org.springframework.beans.factory.support.DisposableBeanAdapter.inferDestroyMethodIfNecessary
   * @see org.springframework.beans.factory.support.DisposableBeanAdapter.CLOSE_METHOD_NAME
   */
  @Throws(Exception::class)
  override fun destroy() {
    //This method forces spring disposer to avoid call of SqlSessionTemplate.close() which gives UnsupportedOperationException
  }

  /**
   * Proxy needed to route MyBatis method calls to the proper SqlSession got
   * from Spring's Transaction Manager
   * It also unwraps exceptions thrown by `Method#invoke(Object, Object...)` to
   * pass a `PersistenceException` to the `PersistenceExceptionTranslator`.
   */
  private inner class SqlSessionInterceptor : InvocationHandler {
    @Throws(Throwable::class)
    override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any? {
      val sqlSession = getSqlSession(
          this@SqlSessionTemplate.sqlSessionFactory,
          this@SqlSessionTemplate.executorType,
          this@SqlSessionTemplate.exceptionTranslator)
      try {
        val result: Any? = method.invoke(sqlSession, *(args ?: arrayOfNulls<Any>(0)))
        if (!isSqlSessionTransactional(sqlSession, this@SqlSessionTemplate.sqlSessionFactory)) {
          // force commit even on non-dirty sessions because some databases require
          // a commit/rollback before calling close()
          sqlSession.commit(true)
        }
        return result
      } catch (t: Throwable) {
        var unwrapped = unwrapThrowable(t)
        if (this@SqlSessionTemplate.exceptionTranslator != null && unwrapped is PersistenceException) {
          // release the connection to avoid a deadlock if the translator is no loaded. See issue #22
          closeSqlSession(sqlSession, this@SqlSessionTemplate.sqlSessionFactory)
          val translated = this@SqlSessionTemplate.exceptionTranslator.translateExceptionIfPossible(unwrapped)
          if (translated != null) {
            unwrapped = translated
          }
        }
        throw unwrapped
      } finally {
        closeSqlSession(sqlSession, this@SqlSessionTemplate.sqlSessionFactory)
      }
    }
  }

}
