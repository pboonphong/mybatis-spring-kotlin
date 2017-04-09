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

import org.apache.ibatis.session.ExecutorType
import org.apache.ibatis.session.SqlSession
import org.springframework.dao.support.PersistenceExceptionTranslator
import org.springframework.transaction.support.ResourceHolderSupport

/**
 * Used to keep current `SqlSession` in `TransactionSynchronizationManager`.
 * The `SqlSessionFactory` that created that `SqlSession` is used as a key.
 * `ExecutorType` is also kept to be able to check if the user is trying to change it
 * during a TX (that is not allowed) and throw a Exception in that case.
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 * @version $Id$
 */
class SqlSessionHolder
/**
 * Creates a new holder instance.
 *
 * @param sqlSession the `SqlSession` has to be hold.
 * @param executorType the `ExecutorType` has to be hold.
 */
(val sqlSession: SqlSession,
 val executorType: ExecutorType,
 val persistenceExceptionTranslator: PersistenceExceptionTranslator?) : ResourceHolderSupport()
