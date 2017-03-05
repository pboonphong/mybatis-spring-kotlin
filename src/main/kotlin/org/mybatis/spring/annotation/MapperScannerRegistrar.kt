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
package org.mybatis.spring.annotation

import java.util.ArrayList

import org.mybatis.spring.mapper.ClassPathMapperScanner
import org.mybatis.spring.mapper.MapperFactoryBean
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanNameGenerator
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.annotation.AnnotationAttributes
import org.springframework.core.io.ResourceLoader
import org.springframework.core.type.AnnotationMetadata
import org.springframework.util.ClassUtils
import org.springframework.util.StringUtils

/**
 * A [ImportBeanDefinitionRegistrar] to allow annotation configuration of
 * MyBatis mapper scanning. Using an @Enable annotation allows beans to be
 * registered via @Component configuration, whereas implementing
 * `BeanDefinitionRegistryPostProcessor` will work for XML configuration.

 * @author Michael Lanyon
 * *
 * @author Eduardo Macarron
 * *
 * *
 * @see MapperFactoryBean

 * @see ClassPathMapperScanner

 * @since 1.2.0
 * *
 * @version $Id$
 */
class MapperScannerRegistrar : ImportBeanDefinitionRegistrar, ResourceLoaderAware {

  private var resourceLoader: ResourceLoader? = null

  /**
   * {@inheritDoc}
   */
  override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {

    val annoAttrs = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(MapperScan::class.java.name))
    val scanner = ClassPathMapperScanner(registry)

    // this check is needed in Spring 3.1
    if (resourceLoader != null) {
      scanner.resourceLoader = resourceLoader!!
    }

    val annotationClass = annoAttrs.getClass<Annotation>("annotationClass")
    if (Annotation::class.java != annotationClass) {
      scanner.setAnnotationClass(annotationClass)
    }

    val markerInterface = annoAttrs.getClass<Any>("markerInterface")
    if (Class::class.java != markerInterface) {
      scanner.setMarkerInterface(markerInterface)
    }

    val generatorClass = annoAttrs.getClass<BeanNameGenerator>("nameGenerator")
    if (BeanNameGenerator::class.java != generatorClass) {
      scanner.setBeanNameGenerator(BeanUtils.instantiateClass(generatorClass))
    }

    val mapperFactoryBeanClass = annoAttrs.getClass<MapperFactoryBean<Any>>("factoryBean")
    if (MapperFactoryBean::class.java != mapperFactoryBeanClass) {
      scanner.setMapperFactoryBean(BeanUtils.instantiateClass(mapperFactoryBeanClass))
    }

    scanner.setSqlSessionTemplateBeanName(annoAttrs.getString("sqlSessionTemplateRef"))
    scanner.setSqlSessionFactoryBeanName(annoAttrs.getString("sqlSessionFactoryRef"))

    val basePackages = ArrayList<String>()
    for (pkg in annoAttrs.getStringArray("value")) {
      if (StringUtils.hasText(pkg)) {
        basePackages.add(pkg)
      }
    }
    for (pkg in annoAttrs.getStringArray("basePackages")) {
      if (StringUtils.hasText(pkg)) {
        basePackages.add(pkg)
      }
    }
    for (clazz in annoAttrs.getClassArray("basePackageClasses")) {
      basePackages.add(ClassUtils.getPackageName(clazz))
    }
    scanner.registerFilters()
    scanner.doScan(*StringUtils.toStringArray(basePackages))
  }

  /**
   * {@inheritDoc}
   */
  override fun setResourceLoader(resourceLoader: ResourceLoader) {
    this.resourceLoader = resourceLoader
  }

}
