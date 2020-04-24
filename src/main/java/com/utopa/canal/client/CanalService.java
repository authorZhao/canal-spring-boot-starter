package com.utopa.canal.client;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author authorZhao
 * @date 2020年04月15日
 */
@Service
@EnableConfigurationProperties({CanalProperties.class})
@Import({CanalFactory.class, CanalSpringListener.class, DefalutRowDataHandler.class})
public class CanalService implements BeanPostProcessor,ApplicationListener<CanalSpringListener> {

    private static final Logger logger = LoggerFactory.getLogger(CanalService.class);

    @Autowired
    private CanalFactory canalFactory;

    private List<CanalMessageListener> canalMessageListeners = new CopyOnWriteArrayList<>();

    private Map<String, CanalMessageListener> canalMessageListenerMap = new ConcurrentHashMap<>();

    public void process() {
        CanalConnector canalConnector = canalFactory.getCanalConnector();

        new Thread(()->{
            int failCount = 0;
            while(true){
                int batchSize = canalFactory.getCanalProperties().getBatchSize();
                Message message = canalConnector.getWithoutAck(batchSize); // 获取指定数量的数据
                long batchId = message.getId();
                int size = message.getEntries().size();
                if (batchId == -1 || size == 0) {
                    try {
                        //logger.info("当前线程：{}，进行休眠",Thread.currentThread().getName());
                        Thread.currentThread().sleep(canalFactory.getCanalProperties().getSleepTime());
                    } catch (InterruptedException e) {
                        logger.error("threadName={},sleep error",Thread.currentThread().getName());
                    }
                } else {
                    try{
                        onMessage(message);

                    }catch (Exception e){
                        if(failCount<=3){
                            logger.info("消费失败，回滚，batchId={}",message.getId());
                            failCount++;
                            canalConnector.rollback();
                        }else{
                            failCount = 0;
                            logger.error("消费失败次数过多，停止消费，停止回滚，batchId={}",message.getId());
                        }

                    }

                    //consumer(message.getEntries());
                }
                canalConnector.ack(batchId); // 提交确认
            }


        }).start();
    }

    private void onMessage(Message message) throws InvocationTargetException, IllegalAccessException {
        List<CanalEntry.Entry> entries = message.getEntries();
        for (CanalEntry.Entry entry : entries) {
            String key = entry.getHeader().getSchemaName()+entry.getHeader().getTableName();
            CanalMessageListener listener = canalMessageListenerMap.get(key);
            if(listener!=null){
                listener.onMessage(entry);
            }
        }
    }


    @Override
    public void onApplicationEvent(CanalSpringListener event) {
        logger.info("开始消费canal数据");
        process();
    }



    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }



    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        //判断有没有@CanalListener注解
        List<Method> collect = Arrays.stream(targetClass.getMethods()).filter(s -> s.isAnnotationPresent(CanalListener.class)).collect(Collectors.toList());
        if (collect==null||collect.size()<=0)return bean;
        // Non-empty set of methods
        collect.forEach(m->processJmsListener(m.getAnnotation(CanalListener.class), m, bean));
        logger.info("正在为{}类注册CanalListener监听器,需要注册的数量为{}",beanName,collect.size());
        return bean;
    }





    private void processJmsListener(CanalListener canalListener, Method method, Object bean) {
        try{
            if(StringUtils.isEmpty(canalListener.databaseName()) || StringUtils.isEmpty(canalListener.tableName()))return;

            Class<?>[] parameterTypes = method.getParameterTypes();
            if(parameterTypes!=null && parameterTypes.length>=2){
                //第二个方法参数必须是类型
                if(CanalEntry.EventType.class != parameterTypes[1])return;
                logger.info("添加CanalListener监听器定义class=,{},method={}",bean.getClass().getName(),method.getName());
                CanalMessageListener canalMessageListener = CanalListenerConsumer.getObject(bean, method, canalListener);
                canalMessageListenerMap.put(canalListener.databaseName()+canalListener.tableName(),canalMessageListener);
                canalMessageListeners.add(canalMessageListener);
            }
        }catch (Exception e){
            logger.error("监听器创建失败");
        }
    }
}
