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

import org.mybatis.spring.mapper.MapperFactoryBean
import org.mybatis.spring.mapper.MapperScannerConfigurer
import org.springframework.beans.factory.support.BeanNameGenerator
import org.springframework.context.annotation.Import
import kotlin.reflect.KClass

/**
 * Use this annotation to register MyBatis mapper interfaces when using Java
 * Config. It performs when same work as [MapperScannerConfigurer] via
 * [MapperScannerRegistrar].
 *
 * Configuration example:
 *
 * &#064;Configuration
 * &#064;MapperScan("org.mybatis.spring.sample.mapper")
 * public class AppConfig {
 *
 * &#064;Bean
 * public DataSource dataSource() {
 * return new EmbeddedDatabaseBuilder()
 * .addScript("schema.sql")
 * .build();
 * }
 *
 * &#064;Bean
 * public DataSourceTransactionManager transactionManager() {
 * return new DataSourceTransactionManager(dataSource());
 * }
 *
 * &#064;Bean
 * public SqlSessionFactory sqlSessionFactory() throws Exception {
 * SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
 * sessionFactory.setDataSource(dataSource());
 * return sessionFactory.getObject();
 * }
 * }
 *
 * @author Michael Lanyon
 * @author Eduardo Macarron
 * @since 1.2.0
 * @see MapperScannerRegistrar
 * @see MapperFactoryBean
 * @version $Id$
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@MustBeDocumented
@Import(MapperScannerRegistrar::class)
annotation class MapperScan(

  /**
     * Alias for the [.basePackages] attribute. Allows for more concise
     * annotation declarations e.g.:
     * `@EnableMyBatisMapperScanner(&quot;org.my.pkg&quot;)` instead of `&quot;org.my.pkg&quot;`)}.
     */
    vararg val value: String = [],

  /**
     * Base packages to scan for MyBatis interfaces. Note that only interfaces
     * with at least one method will be registered; concrete classes will be
     * ignored.
     */
    val basePackages: Array<String> = [],

  /**
     * Type-safe alternative to [.basePackages] for specifying the packages
     * to scan for annotated components. The package of each class specified will be scanned.
     *
     * Consider creating a special no-op marker class or interface in each package
     * that serves no purpose other than being referenced by this attribute.
     */
    val basePackageClasses: Array<KClass<*>> = [],

  /**
     * The [BeanNameGenerator] class to be used for naming detected components
     * within the Spring container.
     */
    val nameGenerator: KClass<out BeanNameGenerator> = BeanNameGenerator::class,

  /**
     * This property specifies the annotation that the scanner will search for.
     *
     *
     * The scanner will register all interfaces in the base package that also have
     * the specified annotation.
     *
     *
     * Note this can be combined with markerInterface.
     */

    val annotationClass: KClass<out Annotation> = Annotation::class,
  /**
     * This property specifies the parent that the scanner will search for.
     *
     * The scanner will register all interfaces in the base package that also have
     * the specified interface class as a parent.
     *
     * Note this can be combined with annotationClass.
     */
    val markerInterface: KClass<*> = Class::class,

  /**
     * Specifies which `SqlSessionTemplate` to use in the case that there is
     * more than one in the spring context. Usually this is only needed when you
     * have more than one datasource.
     */
    val sqlSessionTemplateRef: String = "",

  /**
     * Specifies which `SqlSessionFactory` to use in the case that there is
     * more than one in the spring context. Usually this is only needed when you
     * have more than one datasource.
     */
    val sqlSessionFactoryRef: String = "",

  /**
     * Specifies a custom MapperFactoryBean to return a mybatis proxy as spring bean.
     */
// FIXME: Incompatible types when pass Java class to Kotlin annotation
// @see https://discuss.kotlinlang.org/t/how-to-pass-java-class-as-parameter-to-kotlin-annotation/2374
//    val factoryBean: KClass<out MapperFactoryBean<*>> = MapperFactoryBean::class
    val factoryBean: KClass<*> = MapperFactoryBean::class
)
