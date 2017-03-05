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
package org.mybatis.spring.support

import org.springframework.util.Assert.notNull

import org.apache.ibatis.session.SqlSession
import org.apache.ibatis.session.SqlSessionFactory
import org.mybatis.spring.SqlSessionTemplate
import org.springframework.dao.support.DaoSupport

/**
 * Convenient super class for MyBatis SqlSession data access objects.
 * It gives you access to the template which can then be used to execute SQL methods.
 *
 *
 * This class needs a SqlSessionTemplate or a SqlSessionFactory.
 * If both are set the SqlSessionFactory will be ignored.
 *
 *
 * {code Autowired} was removed from setSqlSessionTemplate and setSqlSessionFactory
 * in version 1.2.0.

 * @author Putthiphong Boonphong
 * *
 * *
 * @see .setSqlSessionFactory

 * @see .setSqlSessionTemplate

 * @see SqlSessionTemplate

 * @version $Id$
 */
abstract class SqlSessionDaoSupport : DaoSupport() {

  /**
   * Users should use this method to get a SqlSession to call its statement methods
   * This is SqlSession is managed by spring. Users should not commit/rollback/close it
   * because it will be automatically done.

   * @return Spring managed thread safe SqlSession
   */
  var sqlSession: SqlSession? = null

  private var externalSqlSession: Boolean = false

  fun setSqlSessionFactory(sqlSessionFactory: SqlSessionFactory) {
    if (!this.externalSqlSession) {
      this.sqlSession = SqlSessionTemplate(sqlSessionFactory)
    }
  }

  fun setSqlSessionTemplate(sqlSessionTemplate: SqlSessionTemplate) {
    this.sqlSession = sqlSessionTemplate
    this.externalSqlSession = true
  }

  /**
   * {@inheritDoc}
   */
  override fun checkDaoConfig() {
    notNull(this.sqlSession, "Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required")
  }

}
