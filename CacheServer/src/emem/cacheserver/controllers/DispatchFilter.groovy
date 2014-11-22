package emem.cacheserver.controllers

import emem.cacheserver.core.CacheConfig

import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Created by hello on 14-11-20.
 */
class DispatchFilter implements Filter {
    private final def mapping = [:]

    @Override
    void init(FilterConfig filterConfig) throws ServletException {
        println 'filter init'

        CacheConfig.getInstance().init(new File('WEB-INF/config/tokens.conf'))

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
    }

    @Override
    void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        def rq = (HttpServletRequest)servletRequest
        def res = (HttpServletResponse)servletResponse

        def fullPath = rq.getServletPath()
        def index = fullPath.lastIndexOf('/')
        def path = fullPath[0..<index]?:'/'
        def method = fullPath.substring(index+1)

        def c
        def metaMethods

        if(!(c=mapping[path]) || !(metaMethods=c.respondsTo(method))) {
            res.sendError(404)
            return;
        }

        metaMethods[0].invoke(c, rq, res)
    }

    @Override
    void destroy() {
        CacheConfig.getInstance().store(new File('WEB-INF/config/tokens.conf'))

        println 'filter destroy'
    }
}
