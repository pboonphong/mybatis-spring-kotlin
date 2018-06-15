package org.mybatis.logging

import org.apache.ibatis.logging.LogFactory

/**
 * LoggerFactory is a wrapper around [LogFactory] to support [Logger].
 *
 * @author Putthiphong Boonphong
 */
object LoggerFactory {

  fun getLogger(aClass: Class<*>): Logger {
    return Logger(LogFactory.getLog(aClass))
  }

  fun getLogger(logger: String): Logger {
    return Logger(LogFactory.getLog(logger))
  }

}
