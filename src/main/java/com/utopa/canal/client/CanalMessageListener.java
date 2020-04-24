package com.utopa.canal.client;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;

import java.lang.reflect.InvocationTargetException;

/**
 * @author authorZhao
 * @date 2020年04月15日
 */

@FunctionalInterface
public interface CanalMessageListener {

    /**
     * 消费同步的数据，以行为单位
     * @param entry 对应数据库的行
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    void onMessage(CanalEntry.Entry entry) throws InvocationTargetException, IllegalAccessException;
}
