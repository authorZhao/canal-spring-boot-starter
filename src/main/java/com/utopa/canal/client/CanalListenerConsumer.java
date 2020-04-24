package com.utopa.canal.client;

import com.alibaba.otter.canal.protocol.CanalEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author authorZhao
 * @date 2020年04月15日
 */
public class CanalListenerConsumer implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(CanalMessageListener.class);

    private CanalListener canalListener;
    private Object bean;
    private Method method;

    //采用构造注入，通过反射执行方法
    public CanalListenerConsumer(Object bean, Method method,CanalListener canalListener) {
        this.bean = bean;
        this.method = method;
        this.canalListener = canalListener;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this,params);
        }
        CanalEntry.Entry entry = (CanalEntry.Entry) params[0];
        if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN || entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND) {
            return null;
        }
        CanalEntry.RowChange rowChage = null;
        try {
            rowChage = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
        } catch (Exception e) {
            throw new RuntimeException("ERROR ## parser of eromanga-event has an error , data:" + entry.toString(),e);
        }
        Class<?> clazz = this.method.getParameterTypes()[0];
        for (CanalEntry.RowData rowData : rowChage.getRowDatasList()) {
            CanalEntry.EventType eventType = rowChage.getEventType();
            try {
                Object param1 = changeType(rowData,canalListener, clazz,eventType);
                return this.method.invoke(bean,param1,  eventType);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private Object changeType(CanalEntry.RowData rowData,CanalListener canalListener, Class<?> clazz, CanalEntry.EventType eventType) {
        RowDataHandler bean = null;
        Class<? extends RowDataHandler> handler = canalListener.handler();
        if(handler!=null){
            bean =  CanalSpringListener.getBean(handler);
        }
        if(bean==null){
            bean = CanalSpringListener.getBean(DefalutRowDataHandler.class);
        }
        return bean.changeType(rowData,clazz,eventType);
    }


    private boolean checkNeedConsumer(CanalListener canalListener, String schemaName, String tableName) {
        if(canalListener==null || StringUtils.isEmpty(schemaName) ||StringUtils.isEmpty(tableName)) return false;
        if(canalListener.databaseName().equals(schemaName) && canalListener.tableName().equals(tableName))return true;
        return false;
    }

    /**
     *
     * @param bean
     * @param method
     * @param canalListener
     * @return
     */
    public static CanalMessageListener getObject(Object bean, Method method,CanalListener canalListener) {
        CanalListenerConsumer myInvocationHandler = new CanalListenerConsumer(bean,method,canalListener);
        ClassLoader classLoader = bean.getClass().getClassLoader();
        Class[] clazz = {CanalMessageListener.class};
        //创建代理对象
        CanalMessageListener object = null;
        try {
            //写死，专门为CanalMessageListener类生成代理对象
            object = (CanalMessageListener)Proxy.newProxyInstance(classLoader, clazz, myInvocationHandler);
        }catch (Exception e){
            e.printStackTrace();
        }
        return object;
    }
}
