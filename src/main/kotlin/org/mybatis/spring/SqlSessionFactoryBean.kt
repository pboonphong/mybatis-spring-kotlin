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

import org.springframework.util.Assert.notNull
import org.springframework.util.Assert.state
import org.springframework.util.ObjectUtils.isEmpty
import org.springframework.util.StringUtils.hasLength
import org.springframework.util.StringUtils.tokenizeToStringArray

import java.io.IOException
import java.sql.SQLException
import java.util.Properties

import javax.sql.DataSource

import org.apache.ibatis.builder.xml.XMLConfigBuilder
import org.apache.ibatis.builder.xml.XMLMapperBuilder
import org.apache.ibatis.cache.Cache
import org.apache.ibatis.executor.ErrorContext
import org.apache.ibatis.io.VFS
import org.mybatis.logging.LoggerFactory
import org.apache.ibatis.mapping.DatabaseIdProvider
import org.apache.ibatis.mapping.Environment
import org.apache.ibatis.plugin.Interceptor
import org.apache.ibatis.reflection.factory.ObjectFactory
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory
import org.apache.ibatis.session.Configuration
import org.apache.ibatis.session.SqlSessionFactory
import org.apache.ibatis.session.SqlSessionFactoryBuilder
import org.apache.ibatis.transaction.TransactionFactory
import org.apache.ibatis.type.TypeHandler
import org.mybatis.spring.transaction.SpringManagedTransactionFactory
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.NestedIOException
import org.springframework.core.io.Resource
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy

/**
 * `FactoryBean` that creates an MyBatis `SqlSessionFactory`.
 * This is the usual way to set up a shared MyBatis `SqlSessionFactory` in a Spring application context;
 * the SqlSessionFactory can then be passed to MyBatis-based DAOs via dependency injection.

 * Either `DataSourceTransactionManager` or `JtaTransactionManager` can be used for transaction
 * demarcation in combination with a `SqlSessionFactory`. JTA should be used for transactions
 * which span multiple databases or when container managed transactions (CMT) are being used.

 * @author Putthiphong Boonphong
 * @author Hunter Presnall
 * @author Eduardo Macarron
 * @author Eddú Meléndez
 * @author Kazuki Shimizu
 *
 * @see .setConfigLocation
 * @see .setDataSource
 *
 * @version $Id$
 */
class SqlSessionFactoryBean : FactoryBean<SqlSessionFactory>, InitializingBean, ApplicationListener<ApplicationEvent> {

  private var configLocation: Resource? = null

  private var configuration: Configuration? = null

  private var mapperLocations: Array<Resource>? = null

  private var dataSource: DataSource? = null

  private var transactionFactory: TransactionFactory? = null

  private var configurationProperties: Properties? = null

  private var sqlSessionFactoryBuilder = SqlSessionFactoryBuilder()

  private var sqlSessionFactory: SqlSessionFactory? = null

  //EnvironmentAware requires spring 3.1
  private var environment = SqlSessionFactoryBean::class.java.simpleName

  private var failFast: Boolean = false

  private var plugins: Array<Interceptor>? = null

  private var typeHandlers: Array<TypeHandler<*>>? = null

  private var typeHandlersPackage: String? = null

  private var typeAliases: Array<Class<*>>? = null

  private var typeAliasesPackage: String? = null

  private var typeAliasesSuperType: Class<*>? = null

  //issue #19. No default provider.
  /**
   * Gets the DatabaseIdProvider
   *
   * @since 1.1.0
   *
   * @return
   */
  /**
   * Sets the DatabaseIdProvider.
   * As of version 1.2.2 this variable is not initialized by default.
   *
   * @since 1.1.0
   */
  var databaseIdProvider: DatabaseIdProvider? = null

  var vfs: Class<out VFS>? = null

  var cache: Cache? = null

  private var objectFactory: ObjectFactory? = null

  private var objectWrapperFactory: ObjectWrapperFactory? = null

  /**
   * Sets the ObjectFactory.

   * @since 1.1.2
   * *
   * @param objectFactory
   */
  fun setObjectFactory(objectFactory: ObjectFactory?) {
    this.objectFactory = objectFactory
  }

  /**
   * Sets the ObjectWrapperFactory.

   * @since 1.1.2
   * *
   * @param objectWrapperFactory
   */
  fun setObjectWrapperFactory(objectWrapperFactory: ObjectWrapperFactory?) {
    this.objectWrapperFactory = objectWrapperFactory
  }

  /**
   * Mybatis plugin list.

   * @since 1.0.1
   * *
   * *
   * @param plugins list of plugins
   */
  fun setPlugins(plugins: Array<Interceptor>?) {
    this.plugins = plugins
  }

  /**
   * Packages to search for type aliases.

   * @since 1.0.1
   * *
   * *
   * @param typeAliasesPackage package to scan for domain objects
   */
  fun setTypeAliasesPackage(typeAliasesPackage: String?) {
    this.typeAliasesPackage = typeAliasesPackage
  }

  /**
   * Super class which domain objects have to extend to have a type alias created.
   * No effect if there is no package to scan configured.

   * @since 1.1.2
   * *
   * *
   * @param typeAliasesSuperType super class for domain objects
   */
  fun setTypeAliasesSuperType(typeAliasesSuperType: Class<*>?) {
    this.typeAliasesSuperType = typeAliasesSuperType
  }

  /**
   * Packages to search for type handlers.

   * @since 1.0.1
   * *
   * *
   * @param typeHandlersPackage package to scan for type handlers
   */
  fun setTypeHandlersPackage(typeHandlersPackage: String?) {
    this.typeHandlersPackage = typeHandlersPackage
  }

  /**
   * Set type handlers. They must be annotated with `MappedTypes` and optionally with `MappedJdbcTypes`

   * @since 1.0.1
   * *
   * *
   * @param typeHandlers Type handler list
   */
  fun setTypeHandlers(typeHandlers: Array<TypeHandler<*>>?) {
    this.typeHandlers = typeHandlers
  }

  /**
   * List of type aliases to register. They can be annotated with `Alias`

   * @since 1.0.1
   * *
   * *
   * @param typeAliases Type aliases list
   */
  fun setTypeAliases(typeAliases: Array<Class<*>>?) {
    this.typeAliases = typeAliases
  }

  /**
   * If true, a final check is done on Configuration to assure that all mapped
   * statements are fully loaded and there is no one still pending to resolve
   * includes. Defaults to false.

   * @since 1.0.1
   * *
   * *
   * @param failFast enable failFast
   */
  fun setFailFast(failFast: Boolean) {
    this.failFast = failFast
  }

  /**
   * Set the location of the MyBatis `SqlSessionFactory` config file. A typical value is
   * "WEB-INF/mybatis-configuration.xml".
   */
  fun setConfigLocation(configLocation: Resource?) {
    this.configLocation = configLocation
  }

  /**
   * Set a customized MyBatis configuration.
   * @param configuration MyBatis configuration
   * *
   * @since 1.3.0
   */
  fun setConfiguration(configuration: Configuration?) {
    this.configuration = configuration
  }

  /**
   * Set locations of MyBatis mapper files that are going to be merged into the `SqlSessionFactory`
   * configuration at runtime.
   * This is an alternative to specifying "&lt;sqlmapper&gt;" entries in an MyBatis config file.
   * This property being based on Spring's resource abstraction also allows for specifying
   * resource patterns here: e.g. "classpath*:sqlmap/\*-mapper.xml".
   */
  fun setMapperLocations(mapperLocations: Array<Resource>?) {
    this.mapperLocations = mapperLocations
  }

  /**
   * Set optional properties to be passed into the SqlSession configuration, as alternative to a
   * `&amp;lt;properties&amp;gt;` tag in the configuration xml file. This will be used to
   * resolve placeholders in the config file.
   */
  fun setConfigurationProperties(sqlSessionFactoryProperties: Properties?) {
    this.configurationProperties = sqlSessionFactoryProperties
  }

  /**
   * Set the JDBC `DataSource` that this instance should manage transactions for. The `DataSource`
   * should match the one used by the `SqlSessionFactory`: for example, you could specify the same
   * JNDI DataSource for both.

   * A transactional JDBC `Connection` for this `DataSource` will be provided to application code
   * accessing this `DataSource` directly via `DataSourceUtils` or `DataSourceTransactionManager`.

   * The `DataSource` specified here should be the target `DataSource` to manage transactions for, not
   * a `TransactionAwareDataSourceProxy`. Only data access code may work with
   * `TransactionAwareDataSourceProxy`, while the transaction manager needs to work on the
   * underlying target `DataSource`. If there's nevertheless a `TransactionAwareDataSourceProxy`
   * passed in, it will be unwrapped to extract its target `DataSource`.

   */
  fun setDataSource(dataSource: DataSource?) {
    if (dataSource is TransactionAwareDataSourceProxy) {
      // If we got a TransactionAwareDataSourceProxy, we need to perform
      // transactions for its underlying target DataSource, else data
      // access code won't see properly exposed transactions (i.e.
      // transactions for the target DataSource).
      this.dataSource = dataSource.targetDataSource
    } else {
      this.dataSource = dataSource
    }
  }

  /**
   * Sets the `SqlSessionFactoryBuilder` to use when creating the `SqlSessionFactory`.

   * This is mainly meant for testing so that mock SqlSessionFactory classes can be injected. By
   * default, `SqlSessionFactoryBuilder` creates `DefaultSqlSessionFactory` instances.

   */
  fun setSqlSessionFactoryBuilder(sqlSessionFactoryBuilder: SqlSessionFactoryBuilder) {
    this.sqlSessionFactoryBuilder = sqlSessionFactoryBuilder
  }

  /**
   * Set the MyBatis TransactionFactory to use. Default is `SpringManagedTransactionFactory`

   * The default `SpringManagedTransactionFactory` should be appropriate for all cases:
   * be it Spring transaction management, EJB CMT or plain JTA. If there is no active transaction,
   * SqlSession operations will execute SQL statements non-transactionally.

   * **It is strongly recommended to use the default `TransactionFactory`.** If not used, any
   * attempt at getting an SqlSession through Spring's MyBatis framework will throw an exception if
   * a transaction is active.

   * @see SpringManagedTransactionFactory

   * @param transactionFactory the MyBatis TransactionFactory
   */
  fun setTransactionFactory(transactionFactory: TransactionFactory?) {
    this.transactionFactory = transactionFactory
  }

  /**
   * **NOTE:** This class *overrides* any `Environment` you have set in the MyBatis
   * config file. This is used only as a placeholder name. The default value is
   * `SqlSessionFactoryBean.class.getSimpleName()`.

   * @param environment the environment name
   */
  fun setEnvironment(environment: String) {
    this.environment = environment
  }

  /**
   * {@inheritDoc}
   */
  @Throws(Exception::class)
  override fun afterPropertiesSet() {
    notNull(dataSource, "Property 'dataSource' is required")
    notNull(sqlSessionFactoryBuilder, "Property 'sqlSessionFactoryBuilder' is required")
    state((configuration == null && configLocation == null) || !(configuration != null && configLocation != null),
        "Property 'configuration' and 'configLocation' can not specified with together")

    this.sqlSessionFactory = buildSqlSessionFactory()
  }

  /**
   * Build a `SqlSessionFactory` instance.
   *
   * The default implementation uses the standard MyBatis `XMLConfigBuilder` API to build a
   * `SqlSessionFactory` instance based on an Reader.
   * Since 1.3.0, it can be specified a [Configuration] instance directly(without config file).
   *
   * @return SqlSessionFactory
   * @throws IOException if loading the config file failed
   */
  @Throws(IOException::class)
  private fun buildSqlSessionFactory(): SqlSessionFactory {
    val configuration: Configuration
    var xmlConfigBuilder: XMLConfigBuilder? = null
    when {
      this.configuration != null -> {
        configuration = this.configuration as Configuration
        when {
          configuration.variables == null -> configuration.variables = this.configurationProperties
          this.configurationProperties != null -> configuration.variables.putAll(this.configurationProperties!!)
        }
      }
      this.configLocation != null -> {
        xmlConfigBuilder = XMLConfigBuilder(this.configLocation!!.inputStream, null, this.configurationProperties)
        configuration = xmlConfigBuilder.configuration
      }
      else -> {
        logger.debug {"Property `configuration` or 'configLocation' not specified, using default MyBatis Configuration"}
        configuration = Configuration()
        if (this.configurationProperties != null) {
          configuration.variables = this.configurationProperties
        }
      }
    }

    if (this.objectFactory != null) {
      configuration.objectFactory = this.objectFactory
    }

    if (this.objectWrapperFactory != null) {
      configuration.objectWrapperFactory = this.objectWrapperFactory
    }

    if (this.vfs != null) {
      configuration.vfsImpl = this.vfs
    }

    if (hasLength(this.typeAliasesPackage)) {
      val typeAliasPackageArray = tokenizeToStringArray(this.typeAliasesPackage,
          ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS)
      for (packageToScan in typeAliasPackageArray) {
        configuration.typeAliasRegistry.registerAliases(packageToScan,
            if (typeAliasesSuperType == null) Any::class.java else typeAliasesSuperType)
        logger.debug {"Scanned package: '$packageToScan' for aliases"}
      }
    }

    if (!isEmpty(this.typeAliases)) {
      for (typeAlias in this.typeAliases!!) {
        configuration.typeAliasRegistry.registerAlias(typeAlias)
        logger.debug {"Registered type alias: '$typeAlias'"}
      }
    }

    if (!isEmpty(this.plugins)) {
      for (plugin in this.plugins!!) {
        configuration.addInterceptor(plugin)
        logger.debug {"Registered plugin: '$plugin'"}
      }
    }

    if (hasLength(this.typeHandlersPackage)) {
      val typeHandlersPackageArray = tokenizeToStringArray(this.typeHandlersPackage,
          ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS)
      for (packageToScan in typeHandlersPackageArray) {
        configuration.typeHandlerRegistry.register(packageToScan)
        logger.debug {"Scanned package: '$packageToScan' for type handlers"}
      }
    }

    if (!isEmpty(this.typeHandlers)) {
      for (typeHandler in this.typeHandlers!!) {
        configuration.typeHandlerRegistry.register(typeHandler)
        logger.debug {"Registered type handler: '$typeHandler'"}
      }
    }

    if (this.databaseIdProvider != null) {//fix #64 set databaseId before parse mapper xmls
      try {
        configuration.databaseId = this.databaseIdProvider!!.getDatabaseId(this.dataSource)
      } catch (e: SQLException) {
        throw NestedIOException("Failed getting a databaseId", e)
      }

    }

    if (this.cache != null) {
      configuration.addCache(this.cache!!)
    }

    if (xmlConfigBuilder != null) {
      try {
        xmlConfigBuilder.parse()
        logger.debug {"Parsed configuration file: '${this.configLocation}'"}
      } catch (ex: Exception) {
        throw NestedIOException("Failed to parse config resource: ${this.configLocation!!}", ex)
      } finally {
        ErrorContext.instance().reset()
      }
    }

    if (this.transactionFactory == null) {
      this.transactionFactory = SpringManagedTransactionFactory()
    }

    configuration.environment = Environment(this.environment, this.transactionFactory!!, this.dataSource!!)

    if (!isEmpty(this.mapperLocations)) {
      for (mapperLocation in this.mapperLocations!!) {
        if (mapperLocation != null) {
          try {
            val xmlMapperBuilder = XMLMapperBuilder(mapperLocation.inputStream,
                configuration, mapperLocation.toString(), configuration.sqlFragments)
            xmlMapperBuilder.parse()
          } catch (e: Exception) {
            throw NestedIOException("Failed to parse mapping resource: '$mapperLocation'", e)
          } finally {
            ErrorContext.instance().reset()
          }
          logger.debug {"Parsed mapper file: '$mapperLocation'"}
        }
      }
    } else {
      logger.debug {"Property 'mapperLocations' was not specified or no matching resources found"}
    }

    return this.sqlSessionFactoryBuilder.build(configuration)
  }

  /**
   * {@inheritDoc}
   */
  @Throws(Exception::class)
  override fun getObject(): SqlSessionFactory {
    if (this.sqlSessionFactory == null) {
      afterPropertiesSet()
    }

    return this.sqlSessionFactory!!
  }

  /**
   * {@inheritDoc}
   */
  override fun getObjectType(): Class<out SqlSessionFactory> {
    return if (this.sqlSessionFactory == null) SqlSessionFactory::class.java else this.sqlSessionFactory!!.javaClass
  }

  /**
   * {@inheritDoc}
   */
  override fun isSingleton(): Boolean {
    return true
  }

  /**
   * {@inheritDoc}
   */
  override fun onApplicationEvent(event: ApplicationEvent) {
    if (failFast && event is ContextRefreshedEvent) {
      // fail-fast -> check all statements are completed
      this.sqlSessionFactory!!.configuration.mappedStatementNames
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(SqlSessionFactoryBean::class.java)
  }

}
