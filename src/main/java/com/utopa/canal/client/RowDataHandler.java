package com.utopa.canal.client;

import com.alibaba.otter.canal.protocol.CanalEntry;

/**
 * 把rowdata转化为pojo对象
 * @author authorZhao
 * @date 2020年04月17日
 */
@FunctionalInterface
public interface RowDataHandler {

    /**
     * 把CanalEntry.RowData转化为对应的对象，注意pojo必须和mysql严格对应
     * @param rowData canal同步的数据，对应行
     * @param clazz 需要转化的pojo对象
     * @param eventType 操作类型
     * @param <T>
     * @return
     */
    <T> T changeType(CanalEntry.RowData rowData, Class<T> clazz, CanalEntry.EventType eventType);

}
