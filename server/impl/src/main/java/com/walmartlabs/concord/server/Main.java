package com.walmartlabs.concord.server;

import com.google.inject.Injector;
import com.walmartlabs.concord.bootstrap.Bootstrap;
import com.walmartlabs.concord.bootstrap.Server;
import com.walmartlabs.concord.server.cfg.LogStoreConfiguration;
import com.walmartlabs.concord.server.security.SecurityModule;
import org.apache.shiro.guice.aop.ShiroAopModule;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class Main {

    public static void main(String[] args) throws Exception {
        Server server = new Server(8001) {
            @Override
            protected void configureServletContext(ServletContextHandler h, Injector i) {
                LogServletConfigurer logCfg = new LogServletConfigurer(i.getInstance(LogStoreConfiguration.class));
                logCfg.configure(h);
            }

            @Override
            protected Injector createInjector(ServletContextHandler h) {
                return Bootstrap.createInjector(Main.class.getClassLoader(),
                        new SecurityModule(h.getServletContext()),
                        new ShiroAopModule());
            }
        };

        server.start();
    }
}
