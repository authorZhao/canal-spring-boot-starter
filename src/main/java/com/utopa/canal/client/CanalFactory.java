package com.utopa.canal.client;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.net.InetSocketAddress;

/**
 * @author authorZhao
 * @date 2020年04月15日
 */
public class CanalFactory implements DisposableBean  {

    private static final Logger logger = LoggerFactory.getLogger(CanalFactory.class);

    private CanalConnector canalConnector;

    @Autowired
    private CanalProperties canalProperties;

    public void setCanalConnector(CanalConnector canalConnector) {
        this.canalConnector = canalConnector;
    }

    public CanalProperties getCanalProperties() {
        return canalProperties;
    }

    public void setCanalProperties(CanalProperties canalProperties) {
        this.canalProperties = canalProperties;
    }

    public CanalConnector getCanalConnector() {
        return canalConnector;
    }

    @Bean
    @ConditionalOnMissingBean(CanalConnector.class)
    public CanalConnector canalConnector(){
        canalConnector = CanalConnectors.newClusterConnector(Lists.newArrayList(
                new InetSocketAddress(canalProperties.getHostname(), canalProperties.getPort())),
                canalProperties.getDestination(),canalProperties.getUsername(),canalProperties.getPassword()
        );
        canalConnector.connect();
        //指定filter，格式{database}.{table}
        if(StringUtils.isNotEmpty(canalProperties.getFilter())){
            canalConnector.subscribe(canalProperties.getFilter());
        }
        //回滚寻找上次中断的为止

        canalConnector.rollback();

        logger.info("canal连接成功");
        return canalConnector;
    }


    @Override
    public void destroy() throws Exception {
        if(canalConnector != null){
            logger.info("canal连接关闭");
            canalConnector.disconnect();
        }
    }

}
