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
package org.mybatis.spring.config

import org.mybatis.spring.mapper.MapperFactoryBean
import org.mybatis.spring.mapper.ClassPathMapperScanner
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.BeanNameGenerator
import org.springframework.beans.factory.xml.BeanDefinitionParser
import org.springframework.beans.factory.xml.ParserContext
import org.springframework.beans.factory.xml.XmlReaderContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.util.StringUtils
import org.w3c.dom.Element

/**
 * A {#code BeanDefinitionParser} that handles the element scan of the MyBatis.
 * namespace
 *
 * @author Lishu Luo
 * @author Eduardo Macarron
 *
 * @since 1.2.0
 * @see MapperFactoryBean
 * @see ClassPathMapperScanner
 * @version $Id$
 */

class MapperScannerBeanDefinitionParser : BeanDefinitionParser {

  /**
   * {@inheritDoc}
   */
  @Synchronized override fun parse(element: Element, parserContext: ParserContext): BeanDefinition? {
    val scanner = ClassPathMapperScanner(parserContext.registry)
    val classLoader = scanner.resourceLoader.classLoader
    val readerContext = parserContext.readerContext
    scanner.resourceLoader = readerContext.resourceLoader
    try {
      val annotationClassName = element.getAttribute(ATTRIBUTE_ANNOTATION)
      if (StringUtils.hasText(annotationClassName)) {
        @Suppress("UNCHECKED_CAST")
        val markerInterface = classLoader.loadClass(annotationClassName) as Class<out Annotation>
        scanner.setAnnotationClass(markerInterface)
      }
      val markerInterfaceClassName = element.getAttribute(ATTRIBUTE_MARKER_INTERFACE)
      if (StringUtils.hasText(markerInterfaceClassName)) {
        val markerInterface = classLoader.loadClass(markerInterfaceClassName)
        scanner.setMarkerInterface(markerInterface)
      }
      val nameGeneratorClassName = element.getAttribute(ATTRIBUTE_NAME_GENERATOR)
      if (StringUtils.hasText(nameGeneratorClassName)) {
        val nameGeneratorClass = classLoader.loadClass(nameGeneratorClassName)
        val nameGenerator = BeanUtils.instantiateClass(nameGeneratorClass, BeanNameGenerator::class.java)
        scanner.setBeanNameGenerator(nameGenerator)
      }
    } catch (ex: Exception) {
      readerContext.error(ex.message, readerContext.extractSource(element), ex.cause)
    }

    val sqlSessionTemplateBeanName = element.getAttribute(ATTRIBUTE_TEMPLATE_REF)
    scanner.setSqlSessionTemplateBeanName(sqlSessionTemplateBeanName)
    val sqlSessionFactoryBeanName = element.getAttribute(ATTRIBUTE_FACTORY_REF)
    scanner.setSqlSessionFactoryBeanName(sqlSessionFactoryBeanName)
    scanner.registerFilters()
    val basePackage = element.getAttribute(ATTRIBUTE_BASE_PACKAGE)
    scanner.scan(*StringUtils.tokenizeToStringArray(basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS))
    return null
  }

  companion object {
    private val ATTRIBUTE_BASE_PACKAGE = "base-package"
    private val ATTRIBUTE_ANNOTATION = "annotation"
    private val ATTRIBUTE_MARKER_INTERFACE = "marker-interface"
    private val ATTRIBUTE_NAME_GENERATOR = "name-generator"
    private val ATTRIBUTE_TEMPLATE_REF = "template-ref"
    private val ATTRIBUTE_FACTORY_REF = "factory-ref"
  }

}
