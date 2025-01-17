package emem.cacheserver.controllers

import com.gmongo.GMongo
import emem.cacheserver.core.CacheConfig
import emem.cacheserver.core.ServerConfig
import emem.cacheserver.rmi.RMIServer
import emem.common.data.SyntaxException

import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Created by hello on 14-11-20.
 */
class DispatchFilter implements Filter {
    private final static logger = ServerConfig.getLogger()

    private final def mapping = [:]

    @Override
    void init(FilterConfig filterConfig) throws ServletException {
        logger.debug 'Dispatch filter init'

        //初始化基本服务器配置
        def tokenDBHost = filterConfig.getInitParameter('tokenDBHost')?:'localhost'
        def tokenDBName = filterConfig.getInitParameter('tokenDBName')?:'emem_system'
        def statDBHost = filterConfig.getInitParameter('statDBHost')?:'localhost'
        def statDBName = filterConfig.getInitParameter('statDBName')?:'stat'
        ServerConfig.tokenDB = new GMongo(tokenDBHost).getDB(tokenDBName)
        ServerConfig.statDB = new GMongo(statDBHost).getDB(statDBName)

        CacheConfig.getInstance().init(ServerConfig.tokenDB.user)

        //配置Controllers
        def pack = 'emem.cacheserver.controllers'
        def dir = new File(getClass().getResource('').getFile())
        dir.list().findAll {it.endsWith('.class')}
        .collect {pack + '.' + it.replaceFirst(/\.class/, '')}
        .collect {Class.forName(it)}
        .findAll {it.getAnnotation(Route)}
        .each {
            def path = it.getAnnotation(Route).value()
            mapping[path] = it.newInstance()
        }

        //enable rmi
        RMIServer.start()

    }

    @Override
    void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        def rq = (HttpServletRequest)servletRequest
        def res = (HttpServletResponse)servletResponse

        def fullPath = rq.getServletPath()
        def index = fullPath.lastIndexOf('/')
        def path = fullPath[0..<index]?:'/'
        def method = fullPath.substring(index+1)

        logger.log "Handle request mapping to $path#$method"

        def c
        def metaMethods

        if(!(c=mapping[path]) || !(metaMethods=c.respondsTo(method))) {
            res.sendError(404)
            return;
        }

        try {
            metaMethods[0].invoke(c, rq, res)
        } catch(SyntaxException ex) {
            res.status = 400
            res.getWriter().print 'Syntax Error'
        } catch(Exception ex) {
            ex.printStackTrace()
            res.status = 500
            res.getWriter().print 'Internal Server Error'
        }

    }

    @Override
    void destroy() {
        logger.debug 'Dispatch filter destroy'
    }
}
