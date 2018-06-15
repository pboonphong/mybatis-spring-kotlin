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
package org.mybatis.spring.mapper

import org.springframework.util.Assert.notNull

import org.apache.ibatis.executor.ErrorContext
import org.mybatis.spring.SqlSessionTemplate
import org.mybatis.spring.support.SqlSessionDaoSupport
import org.springframework.beans.factory.FactoryBean

/**
 * BeanFactory that enables injection of MyBatis mapper interfaces. It can be set up with a
 * SqlSessionFactory or a pre-configured SqlSessionTemplate.
 *
 *
 * Sample configuration:
 * `&lt;bean id=&quot;baseMapper&quot; class=&quot;org.mybatis.spring.mapper.MapperFactoryBean&quot; abstract=&quot;true&quot; lazy-init=&quot;true&quot;&gt;
 * &lt;property name=&quot;sqlSessionFactory&quot; ref=&quot;sqlSessionFactory&quot; /&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id=&quot;oneMapper&quot; parent=&quot;baseMapper&quot;&gt;
 * &lt;property name=&quot;mapperInterface&quot; value=&quot;my.package.MyMapperInterface&quot; /&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id=&quot;anotherMapper&quot; parent=&quot;baseMapper&quot;&gt;
 * &lt;property name=&quot;mapperInterface&quot; value=&quot;my.package.MyAnotherMapperInterface&quot; /&gt;
 * &lt;/bean&gt;`
 *
 * Note that this factory can only inject *interfaces*, not concrete classes.
 *
 * @author Eduardo Macarron
 * @see SqlSessionTemplate
 * @version $Id$
 */
open class MapperFactoryBean<T : Any> : SqlSessionDaoSupport, FactoryBean<T> {

  /**
   * Returns the mapper interface of the MyBatis mapper
   */
  var mapperInterface: Class<T>? = null

  /**
   * Return the flag for addition into MyBatis config.
   *
   * If addToConfig is false the mapper will not be added to MyBatis. This means
   * it must have been included in mybatis-config.xml.
   *
   * If it is true, the mapper will be added to MyBatis in the case it is not already
   * registered.
   *
   * By default addToConfig is true.
   */
  var addToConfig = true

  constructor() {
    //intentionally empty
  }

  constructor(mapperInterface: Class<T>) {
    this.mapperInterface = mapperInterface
  }

  constructor(mapperInterface: Class<T>, addToConfig: Boolean) {
    this.mapperInterface = mapperInterface
    this.addToConfig = addToConfig
  }

  /**
   * {@inheritDoc}
   */
  override fun checkDaoConfig() {
    super.checkDaoConfig()

    notNull(this.mapperInterface, "Property 'mapperInterface' is required")

    val configuration = getSqlSessionTemplate()!!.configuration
    if (this.addToConfig && !configuration.hasMapper(this.mapperInterface)) {
      try {
        configuration.addMapper(this.mapperInterface)
      } catch (e: Exception) {
        logger.error("Error while adding the mapper '" + this.mapperInterface + "' to configuration.", e)
        throw IllegalArgumentException(e)
      } finally {
        ErrorContext.instance().reset()
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Throws(Exception::class)
  override fun getObject(): T {
    return getSqlSessionTemplate()!!.getMapper(this.mapperInterface!!)
  }

  /**
   * {@inheritDoc}
   */
  override fun getObjectType(): Class<T>? {
    return this.mapperInterface
  }

  /**
   * {@inheritDoc}
   */
  override fun isSingleton(): Boolean {
    return true
  }
}
