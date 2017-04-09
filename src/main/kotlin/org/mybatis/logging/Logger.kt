package org.mybatis.logging

import org.apache.ibatis.logging.Log

/**
 * Wrapper of [Log], allow log with lambda expressions.
 *
 * @author Putthiphong Boonphong
 */
class Logger internal constructor(private val log: Log) {

  fun error(s: () -> String, e: Throwable) {
    log.error(s.invoke(), e)
  }

  fun error(s: () -> String) {
    log.error(s.invoke())
  }

  fun warn(s: () -> String) {
    log.warn(s.invoke())
  }

  fun debug(s: () -> String) {
    if (log.isDebugEnabled) {
      log.debug(s.invoke())
    }
  }

  fun trace(s: () -> String) {
    if (log.isTraceEnabled) {
      log.trace(s.invoke())
    }
  }

}
