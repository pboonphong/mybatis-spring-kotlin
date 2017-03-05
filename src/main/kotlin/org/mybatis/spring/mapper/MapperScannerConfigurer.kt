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

import org.apache.ibatis.session.SqlSessionFactory
import org.mybatis.spring.SqlSessionTemplate
import org.springframework.beans.PropertyValue
import org.springframework.beans.PropertyValues
import org.springframework.beans.factory.BeanNameAware
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.config.PropertyResourceConfigurer
import org.springframework.beans.factory.config.TypedStringValue
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.beans.factory.support.BeanNameGenerator
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.util.StringUtils

/**
 * BeanDefinitionRegistryPostProcessor that searches recursively starting from a base package for
 * interfaces and registers them as `MapperFactoryBean`. Note that only interfaces with at
 * least one method will be registered; concrete classes will be ignored.
 *
 *
 * This class was a {code BeanFactoryPostProcessor} until 1.0.1 version. It changed to
 * `BeanDefinitionRegistryPostProcessor` in 1.0.2. See https://jira.springsource.org/browse/SPR-8269
 * for the details.
 *
 *
 * The `basePackage` property can contain more than one package name, separated by either
 * commas or semicolons.
 *
 *
 * This class supports filtering the mappers created by either specifying a marker interface or an
 * annotation. The `annotationClass` property specifies an annotation to search for. The
 * `markerInterface` property specifies a parent interface to search for. If both properties
 * are specified, mappers are added for interfaces that match *either* criteria. By default,
 * these two properties are null, so all interfaces in the given `basePackage` are added as
 * mappers.
 *
 *
 * This configurer enables autowire for all the beans that it creates so that they are
 * automatically autowired with the proper `SqlSessionFactory` or `SqlSessionTemplate`.
 * If there is more than one `SqlSessionFactory` in the application, however, autowiring
 * cannot be used. In this case you must explicitly specify either an `SqlSessionFactory` or
 * an `SqlSessionTemplate` to use via the *bean name* properties. Bean names are used
 * rather than actual objects because Spring does not initialize property placeholders until after
 * this class is processed.
 *
 *
 * Passing in an actual object which may require placeholders (i.e. DB user password) will fail.
 * Using bean names defers actual object creation until later in the startup
 * process, after all placeholder substituation is completed. However, note that this configurer
 * does support property placeholders of its *own* properties. The `basePackage`
 * and bean name properties all support `${property}` style substitution.
 *
 *
 * Configuration sample:
 *
 *

 *
 * `&lt;bean class=&quot;org.mybatis.spring.mapper.MapperScannerConfigurer&quot;&gt;
 * &lt;property name=&quot;basePackage&quot; value=&quot;org.mybatis.spring.sample.mapper&quot; /&gt;
 * &lt;!-- optional unless there are multiple session factories defined --&gt;
 * &lt;property name=&quot;sqlSessionFactoryBeanName&quot; value=&quot;sqlSessionFactory&quot; /&gt;
 * &lt;/bean&gt;
` *
 *

 * @author Hunter Presnall
 * *
 * @author Eduardo Macarron
 * *
 * *
 * @see MapperFactoryBean

 * @see ClassPathMapperScanner

 * @version $Id$
 */
class MapperScannerConfigurer : BeanDefinitionRegistryPostProcessor, InitializingBean, ApplicationContextAware, BeanNameAware {

  private var basePackage: String? = null

  private var addToConfig = true

  private var sqlSessionFactory: SqlSessionFactory? = null

  private var sqlSessionTemplate: SqlSessionTemplate? = null

  private var sqlSessionFactoryBeanName: String? = null

  private var sqlSessionTemplateBeanName: String? = null

  private var annotationClass: Class<out Annotation>? = null

  private var markerInterface: Class<*>? = null

  private var applicationContext: ApplicationContext? = null

  private var beanName: String? = null

  private var processPropertyPlaceHolders: Boolean = false

  /**
   * Gets beanNameGenerator to be used while running the scanner.

   * @return the beanNameGenerator BeanNameGenerator that has been configured
   * *
   * @since 1.2.0
   */
  /**
   * Sets beanNameGenerator to be used while running the scanner.

   * @param nameGenerator the beanNameGenerator to set
   * *
   * @since 1.2.0
   */
  var nameGenerator: BeanNameGenerator? = null

  /**
   * This property lets you set the base package for your mapper interface files.
   *
   *
   * You can set more than one package by using a semicolon or comma as a separator.
   *
   *
   * Mappers will be searched for recursively starting in the specified package(s).

   * @param basePackage base package name
   */
  fun setBasePackage(basePackage: String) {
    this.basePackage = basePackage
  }

  /**
   * Same as `MapperFactoryBean#setAddToConfig(boolean)`.

   * @param addToConfig
   * *
   * @see MapperFactoryBean.setAddToConfig
   */
  fun setAddToConfig(addToConfig: Boolean) {
    this.addToConfig = addToConfig
  }

  /**
   * This property specifies the annotation that the scanner will search for.
   *
   *
   * The scanner will register all interfaces in the base package that also have the
   * specified annotation.
   *
   *
   * Note this can be combined with markerInterface.

   * @param annotationClass annotation class
   */
  fun setAnnotationClass(annotationClass: Class<out Annotation>) {
    this.annotationClass = annotationClass
  }

  /**
   * This property specifies the parent that the scanner will search for.
   *
   *
   * The scanner will register all interfaces in the base package that also have the
   * specified interface class as a parent.
   *
   *
   * Note this can be combined with annotationClass.

   * @param superClass parent class
   */
  fun setMarkerInterface(superClass: Class<*>) {
    this.markerInterface = superClass
  }

  /**
   * Specifies which `SqlSessionTemplate` to use in the case that there is
   * more than one in the spring context. Usually this is only needed when you
   * have more than one datasource.
   *
   *
   * @param sqlSessionTemplate
   */
  @Deprecated("Use {@link #setSqlSessionTemplateBeanName(String)} instead\n   \n    ")
  fun setSqlSessionTemplate(sqlSessionTemplate: SqlSessionTemplate) {
    this.sqlSessionTemplate = sqlSessionTemplate
  }

  /**
   * Specifies which `SqlSessionTemplate` to use in the case that there is
   * more than one in the spring context. Usually this is only needed when you
   * have more than one datasource.
   *
   *
   * Note bean names are used, not bean references. This is because the scanner
   * loads early during the start process and it is too early to build mybatis
   * object instances.

   * @since 1.1.0
   * *
   * *
   * @param sqlSessionTemplateName Bean name of the `SqlSessionTemplate`
   */
  fun setSqlSessionTemplateBeanName(sqlSessionTemplateName: String) {
    this.sqlSessionTemplateBeanName = sqlSessionTemplateName
  }

  /**
   * Specifies which `SqlSessionFactory` to use in the case that there is
   * more than one in the spring context. Usually this is only needed when you
   * have more than one datasource.
   *
   *
   * @param sqlSessionFactory
   */
  @Deprecated("Use {@link #setSqlSessionFactoryBeanName(String)} instead.\n   \n    ")
  fun setSqlSessionFactory(sqlSessionFactory: SqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory
  }

  /**
   * Specifies which `SqlSessionFactory` to use in the case that there is
   * more than one in the spring context. Usually this is only needed when you
   * have more than one datasource.
   *
   *
   * Note bean names are used, not bean references. This is because the scanner
   * loads early during the start process and it is too early to build mybatis
   * object instances.

   * @since 1.1.0
   * *
   * *
   * @param sqlSessionFactoryName Bean name of the `SqlSessionFactory`
   */
  fun setSqlSessionFactoryBeanName(sqlSessionFactoryName: String) {
    this.sqlSessionFactoryBeanName = sqlSessionFactoryName
  }

  /**

   * @since 1.1.1
   * *
   * *
   * @param processPropertyPlaceHolders
   */
  fun setProcessPropertyPlaceHolders(processPropertyPlaceHolders: Boolean) {
    this.processPropertyPlaceHolders = processPropertyPlaceHolders
  }

  /**
   * {@inheritDoc}
   */
  override fun setApplicationContext(applicationContext: ApplicationContext) {
    this.applicationContext = applicationContext
  }

  /**
   * {@inheritDoc}
   */
  override fun setBeanName(name: String) {
    this.beanName = name
  }

  /**
   * {@inheritDoc}
   */
  @Throws(Exception::class)
  override fun afterPropertiesSet() {
    notNull(this.basePackage, "Property 'basePackage' is required")
  }

  /**
   * {@inheritDoc}
   */
  override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
    // left intentionally blank
  }

  /**
   * {@inheritDoc}

   * @since 1.0.2
   */
  override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
    if (this.processPropertyPlaceHolders) {
      processPropertyPlaceHolders()
    }

    val scanner = ClassPathMapperScanner(registry)
    scanner.setAddToConfig(this.addToConfig)
    scanner.setAnnotationClass(this.annotationClass)
    scanner.setMarkerInterface(this.markerInterface)
    scanner.setSqlSessionFactory(this.sqlSessionFactory)
    scanner.setSqlSessionTemplate(this.sqlSessionTemplate)
    scanner.setSqlSessionFactoryBeanName(this.sqlSessionFactoryBeanName)
    scanner.setSqlSessionTemplateBeanName(this.sqlSessionTemplateBeanName)
    scanner.resourceLoader = this.applicationContext
    scanner.setBeanNameGenerator(this.nameGenerator)
    scanner.registerFilters()
    scanner.scan(*StringUtils.tokenizeToStringArray(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS))
  }

  /*
   * BeanDefinitionRegistries are called early in application startup, before
   * BeanFactoryPostProcessors. This means that PropertyResourceConfigurers will not have been
   * loaded and any property substitution of this class' properties will fail. To avoid this, find
   * any PropertyResourceConfigurers defined in the context and run them on this class' bean
   * definition. Then update the values.
   */
  private fun processPropertyPlaceHolders() {
    val prcs = applicationContext!!.getBeansOfType(PropertyResourceConfigurer::class.java)

    if (!prcs.isEmpty() && applicationContext is GenericApplicationContext) {
      val mapperScannerBean = (applicationContext as GenericApplicationContext).beanFactory.getBeanDefinition(beanName)

      // PropertyResourceConfigurer does not expose any methods to explicitly perform
      // property placeholder substitution. Instead, create a BeanFactory that just
      // contains this mapper scanner and post process the factory.
      val factory = DefaultListableBeanFactory()
      factory.registerBeanDefinition(beanName, mapperScannerBean)

      for (prc in prcs.values) {
        prc.postProcessBeanFactory(factory)
      }

      val values = mapperScannerBean.propertyValues

      this.basePackage = updatePropertyValue("basePackage", values)
      this.sqlSessionFactoryBeanName = updatePropertyValue("sqlSessionFactoryBeanName", values)
      this.sqlSessionTemplateBeanName = updatePropertyValue("sqlSessionTemplateBeanName", values)
    }
  }

  private fun updatePropertyValue(propertyName: String, values: PropertyValues): String? {
    val property = values.getPropertyValue(propertyName) ?: return null

    val value = property.value

    when (value) {
      null -> return null
      is String -> return value.toString()
      is TypedStringValue -> return value.value
      else -> return null
    }
  }

}
