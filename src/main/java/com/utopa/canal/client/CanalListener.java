package com.utopa.canal.client;

import java.lang.annotation.*;

/**
 * @author authorZhao
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CanalListener {
    /**
     * 实例id
     * @return
     */
    String id() default "";

    /**
     * 库名
     * @return
     */
    String databaseName();

    /**
     * 表名
     * @return
     */
    String tableName();

    /**
     * 数据转化类，该类必须交给spring管理
     * @return
     */
    Class<? extends RowDataHandler> handler() default DefalutRowDataHandler.class;
}
