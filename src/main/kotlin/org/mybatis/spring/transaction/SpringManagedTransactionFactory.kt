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

import java.sql.Connection
import java.util.Properties

import javax.sql.DataSource

import org.apache.ibatis.session.TransactionIsolationLevel
import org.apache.ibatis.transaction.Transaction
import org.apache.ibatis.transaction.TransactionFactory

/**
 * Creates a `SpringManagedTransaction`.
 *
 * @author Hunter Presnall
 * @version $Id$
 */
class SpringManagedTransactionFactory : TransactionFactory {

  /**
   * {@inheritDoc}
   */
  override fun newTransaction(dataSource: DataSource, level: TransactionIsolationLevel?, autoCommit: Boolean): Transaction {
    return SpringManagedTransaction(dataSource)
  }

  /**
   * {@inheritDoc}
   */
  override fun newTransaction(conn: Connection): Transaction {
    throw UnsupportedOperationException("New Spring transactions require a DataSource")
  }

  /**
   * {@inheritDoc}
   */
  override fun setProperties(props: Properties) {
    // not needed in this version
  }

}
