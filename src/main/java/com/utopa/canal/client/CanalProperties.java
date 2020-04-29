package com.utopa.canal.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author authorZhao
 * @date 2020年04月15日
 */

@Configuration
@ConditionalOnProperty(value = "enabled", matchIfMissing = true)
@ConfigurationProperties(prefix = "com.utopa.canal")
public class CanalProperties {


    /**
     * cancal服务ip
     */
    private String hostname;

    /**
     * 端口
     */
    private Integer port = 11111;

    /**
     * 名字
     */
    private String destination="example";

    /**
     * canal服务端用户名
     */
    private String username;
    /**
     * canal服务端密码
     */
    private String password;

    /**
     * 数据库表名过滤规则
     */
    private String filter;

    /**
     * 获取不到数据，休眠时间
     */
    private long sleepTime;

    /**
     * 一次取出多少条记录
     */
    private Integer batchSize = 1000;


    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public long getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(long sleepTime) {
        this.sleepTime = sleepTime;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public String toString() {
        return "CanalProperties{" +
                "hostname='" + hostname + '\'' +
                ", port=" + port +
                ", destination='" + destination + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", filter='" + filter + '\'' +
                ", sleepTime=" + sleepTime +
                ", batchSize=" + batchSize +
                '}';
    }
}
