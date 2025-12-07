package com.intellilib.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextProvider implements ApplicationContextAware {
    
    private static ApplicationContext context;
    
    public static ApplicationContext getApplicationContext() {
        return context;
    }
    
    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        context = ctx;
        FXMLLoaderUtil.setApplicationContext(ctx); // Initialize FXMLLoaderUtil
    }

    
    
    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }
}