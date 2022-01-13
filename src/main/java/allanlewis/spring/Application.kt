package allanlewis.spring

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.log4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J

@SpringBootApplication
open class Application : InitializingBean {

    private val logger = LoggerFactory.getLogger(Application::class.java)

    override fun afterPropertiesSet() {
        println("logged via system.out")
        System.err.println("logged via system.err")

        val julLogger = java.util.logging.Logger.getLogger(Application::class.java.name)
        julLogger.info("logged via jul")

        val log4JLogger: Logger = Logger.getLogger(Application::class.java)
        log4JLogger.info("logged via log4j")

        val jclLogger: Log = LogFactory.getLog(Application::class.java)
        jclLogger.info("logged via jcl")

        logger.info("logged via slf4j")
    }

}

fun main(args: Array<String>) {

    SysOutOverSLF4J.sendSystemOutAndErrToSLF4J()
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    runApplication<Application>(*args)
}
