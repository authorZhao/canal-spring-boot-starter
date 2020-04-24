package com.utopa.canal.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * @author authorZhao
 * @date 2020年04月16日
 */
public class CanalSpringListener extends ContextRefreshedEvent implements ApplicationContextAware,ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(CanalSpringListener.class);

    private static ApplicationContext applicationContext;

    public CanalSpringListener(ApplicationContext source) {
        super(source);
    }

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        logger.info("项目启动成功，开始派发CanalSpringListener事件");
        CanalSpringListener.applicationContext.publishEvent(this);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        CanalSpringListener.applicationContext=applicationContext;
    }

    public static <T> T getBean(Class<T> clazz){
        return CanalSpringListener.applicationContext.getBean(clazz);
    }

}
