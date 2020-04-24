package com.utopa.canal.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.otter.canal.protocol.CanalEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模式数据转换类
 * @author authorZhao
 * @date 2020年04月20日
 */
public class DefalutRowDataHandler implements RowDataHandler {

    /**
     * 默认转化方式，目标对象必须和数据库字段一一对应，目标对象必须是驼峰命名规则
     * @param rowData canal同步的数据，对应行
     * @param clazz 需要转化的pojo对象
     * @param eventType 操作类型
     * @param <T>
     * @return
     */
    @Override
    public  <T> T changeType(CanalEntry.RowData rowData, Class<T> clazz, CanalEntry.EventType eventType) {
        Map<String,String> camalMap = new HashMap<>();
        List<CanalEntry.Column> rowdatatas = rowData.getAfterColumnsList();
        if(CanalEntry.EventType.DELETE==eventType){
            rowdatatas = rowData.getBeforeColumnsList();
        }
        rowdatatas.stream().forEach(i->{
            String columnName = i.getName();
            String s = StringUtils.underline2camel(columnName);
            camalMap.put(s,i.getValue());

        });
        String sourceJson = JSON.toJSONString(camalMap);
        return JSON.parseObject(sourceJson,clazz);
    }

}
