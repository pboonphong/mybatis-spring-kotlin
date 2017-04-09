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

import java.io.IOException
import java.util.Arrays

import org.apache.ibatis.session.SqlSessionFactory
import org.mybatis.spring.SqlSessionTemplate
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.BeanDefinitionHolder
import org.springframework.beans.factory.config.RuntimeBeanReference
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.GenericBeanDefinition
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.type.classreading.MetadataReader
import org.springframework.core.type.classreading.MetadataReaderFactory
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.core.type.filter.TypeFilter
import org.springframework.util.StringUtils

/**
 * A [ClassPathBeanDefinitionScanner] that registers Mappers by
 * `basePackage`, `annotationClass`, or `markerInterface`. If
 * an `annotationClass` and/or `markerInterface` is specified, only
 * the specified types will be searched (searching for all interfaces will be
 * disabled).
 *
 * This functionality was previously a private class of
 * [MapperScannerConfigurer], but was broken out in version 1.2.0.
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 * @see MapperFactoryBean
 * @since 1.2.0
 * @version $Id$
 */
class ClassPathMapperScanner(registry: BeanDefinitionRegistry) : ClassPathBeanDefinitionScanner(registry, false) {

  private var addToConfig = true

  private var sqlSessionFactory: SqlSessionFactory? = null

  private var sqlSessionTemplate: SqlSessionTemplate? = null

  private var sqlSessionTemplateBeanName: String? = null

  private var sqlSessionFactoryBeanName: String? = null

  private var annotationClass: Class<out Annotation>? = null

  private var markerInterface: Class<*>? = null

  private var mapperFactoryBean: MapperFactoryBean<*> = MapperFactoryBean<Any>()

  fun setAddToConfig(addToConfig: Boolean) {
    this.addToConfig = addToConfig
  }

  fun setAnnotationClass(annotationClass: Class<out Annotation>?) {
    this.annotationClass = annotationClass
  }

  fun setMarkerInterface(markerInterface: Class<*>?) {
    this.markerInterface = markerInterface
  }

  fun setSqlSessionFactory(sqlSessionFactory: SqlSessionFactory?) {
    this.sqlSessionFactory = sqlSessionFactory
  }

  fun setSqlSessionTemplate(sqlSessionTemplate: SqlSessionTemplate?) {
    this.sqlSessionTemplate = sqlSessionTemplate
  }

  fun setSqlSessionTemplateBeanName(sqlSessionTemplateBeanName: String?) {
    this.sqlSessionTemplateBeanName = sqlSessionTemplateBeanName
  }

  fun setSqlSessionFactoryBeanName(sqlSessionFactoryBeanName: String?) {
    this.sqlSessionFactoryBeanName = sqlSessionFactoryBeanName
  }

  fun setMapperFactoryBean(mapperFactoryBean: MapperFactoryBean<*>?) {
    this.mapperFactoryBean = mapperFactoryBean ?: MapperFactoryBean<Any>()
  }

  /**
   * Configures parent scanner to search for the right interfaces. It can search
   * for all interfaces or just for those that extends a markerInterface or/and
   * those annotated with the annotationClass
   */
  fun registerFilters() {
    var acceptAllInterfaces = true

    // if specified, use the given annotation and / or marker interface
    if (this.annotationClass != null) {
      addIncludeFilter(AnnotationTypeFilter(this.annotationClass!!))
      acceptAllInterfaces = false
    }

    // override AssignableTypeFilter to ignore matches on the actual marker interface
    if (this.markerInterface != null) {
      addIncludeFilter(object : AssignableTypeFilter(this.markerInterface) {
        override fun matchClassName(className: String?): Boolean {
          return false
        }
      })
      acceptAllInterfaces = false
    }

    if (acceptAllInterfaces) {
      // default include filter that accepts all classes
      addIncludeFilter { metadataReader, metadataReaderFactory -> true }
    }

    // exclude package-info.java
    addExcludeFilter { metadataReader, metadataReaderFactory ->
      val className = metadataReader.classMetadata.className
      className.endsWith("package-info")
    }
  }

  /**
   * Calls the parent search that will search and register all the candidates.
   * Then the registered objects are post processed to set them as
   * MapperFactoryBeans
   */
  public override fun doScan(vararg basePackages: String): Set<BeanDefinitionHolder> {
    val beanDefinitions = super.doScan(*basePackages)

    if (beanDefinitions.isEmpty()) {
      logger.warn("No MyBatis mapper was found in '${Arrays.toString(basePackages)}' package. Please check your configuration.")
    } else {
      processBeanDefinitions(beanDefinitions)
    }

    return beanDefinitions
  }

  private fun processBeanDefinitions(beanDefinitions: Set<BeanDefinitionHolder>) {
    var definition: GenericBeanDefinition
    for (holder in beanDefinitions) {
      definition = holder.beanDefinition as GenericBeanDefinition

      if (logger.isDebugEnabled) {
        logger.debug("Creating MapperFactoryBean with name '" + holder.beanName
            + "' and '" + definition.beanClassName + "' mapperInterface")
      }

      // the mapper interface is the original class of the bean
      // but, the actual class of the bean is MapperFactoryBean
      definition.constructorArgumentValues.addGenericArgumentValue(definition.beanClassName) // issue #59
      definition.beanClass = this.mapperFactoryBean.javaClass

      definition.propertyValues.add("addToConfig", this.addToConfig)

      var explicitFactoryUsed = false
      if (StringUtils.hasText(this.sqlSessionFactoryBeanName)) {
        definition.propertyValues.add("sqlSessionFactory", RuntimeBeanReference(this.sqlSessionFactoryBeanName))
        explicitFactoryUsed = true
      } else if (this.sqlSessionFactory != null) {
        definition.propertyValues.add("sqlSessionFactory", this.sqlSessionFactory)
        explicitFactoryUsed = true
      }

      if (StringUtils.hasText(this.sqlSessionTemplateBeanName)) {
        if (explicitFactoryUsed) {
          logger.warn("Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.")
        }
        definition.propertyValues.add("sqlSessionTemplate", RuntimeBeanReference(this.sqlSessionTemplateBeanName))
        explicitFactoryUsed = true
      } else if (this.sqlSessionTemplate != null) {
        if (explicitFactoryUsed) {
          logger.warn("Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.")
        }
        definition.propertyValues.add("sqlSessionTemplate", this.sqlSessionTemplate)
        explicitFactoryUsed = true
      }

      if (!explicitFactoryUsed) {
        if (logger.isDebugEnabled) {
          logger.debug("Enabling autowire by type for MapperFactoryBean with name '" + holder.beanName + "'.")
        }
        definition.autowireMode = AbstractBeanDefinition.AUTOWIRE_BY_TYPE
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean {
    return beanDefinition.metadata.isInterface && beanDefinition.metadata.isIndependent
  }

  /**
   * {@inheritDoc}
   */
  override fun checkCandidate(beanName: String, beanDefinition: BeanDefinition): Boolean {
    if (super.checkCandidate(beanName, beanDefinition)) {
      return true
    } else {
      logger.warn("Skipping MapperFactoryBean with name '" + beanName
          + "' and '" + beanDefinition.beanClassName + "' mapperInterface"
          + ". Bean already defined with the same name!")
      return false
    }
  }

}
