canal使用
[canal官网介绍](https://github.com/alibaba/canal/wiki/AdminGuide)

## 1.开启mysql的bin-log

### windows

```

[mysqld]

# 设置mysql的安装目录[根据本地情况进行修改]
basedir=D:/mysql/mysql-5.7.24-winx64
#Path to the database root
datadir=D:/mysql/mysql-5.7.24-winx64/data
#设置3306端口
port = 3306
# 允许最大连接数
max_connections=200
# 服务端使用的字符集默认为8比特编码的latin1字符集
character-set-server=utf8
# 创建新表时将使用的默认存储引擎
default-storage-engine=INNODB
sql_mode=NO_ENGINE_SUBSTITUTION,STRICT_TRANS_TABLES
# Server Id.数据库服务器id，这个id用来在主从服务器中标记唯一mysql服务器
# 开启binlog日志
log-bin = D:/mysql/mysql-5.7.24-winx64/mysql-bin/mysql-bin.log
expire-logs-days = 14
max-binlog-size = 500M
server-id = 1

[mysql]
# 设置mysql客户端默认字符集

default-character-set=utf8
max_allowed_packet=20M


```

重启服务

```
net stop mysql
net start mysql
```

### centos7

[虚拟机搭建mysql8]( https://blog.csdn.net/weixin_43328357/article/details/104357246 )



```sql
注意点：
1.mysql8密码加密方式有点不一样，不清楚的百度，这个设计远程连接问题
2.创建用canal，分配权限
3.binlog日志配置和windows一样的，expire-logs-days = 14
max-binlog-size = 500M
server-id = 1
create user '创建用canal'@'%' identified by 'canal';
GRANT SHOW VIEW, SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%'; 
```



## 2.下载canal

1.[下载地址]( https://github.com/alibaba/canal/releases/tag/canal-1.1.4 )

2.参数配置,使用默认的不用配置，配置一下数据库账号密码

```
1.canal.properties
这里面配置实例
默认有一个example，需要增加实例的可以配置（实例，我的理解就是一个mysql服务器）可不配
canal.destinations = example


```

```
vim example/instance.properties
# 没有改变的就没有贴出来，注意mysql的用户名和密码
canal.instance.master.address=192.168.58.131:3306
# 默认监听的数据库
canal.instance.defaultDatabaseName=open_md
```

3.启动

```
bin/start.sh
# 可以去logs下面查看是否启动成功
```



4.简单demo

```java
// 这个在网上抄袭的
class Test1 {

    @Test
    void contextLoads() {
        // 创建链接                                                                       这个ip是你虚拟机的ip
        CanalConnector connector = CanalConnectors.newSingleConnector(new InetSocketAddress("192.168.58.131",11111), "example", "", "");
        int batchSize = 1000;
        int emptyCount = 0;
        try {
            connector.connect();
            //注意这个相当于一个白名单，那些表需要被监听，这里设置或股改服务端的设置，规则database.tableName
            //connector.subscribe(".*\\..*");
            connector.rollback();
            int totalEmptyCount = 120;
            while (emptyCount < totalEmptyCount) {
                Message message = connector.getWithoutAck(batchSize); // 获取指定数量的数据
                long batchId = message.getId();
                int size = message.getEntries().size();
                if (batchId == -1 || size == 0) {
                    emptyCount++;
                    //System.out.println("empty count : " + emptyCount);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                } else {
                    emptyCount = 0;
                    printEntry(message.getEntries());
                }

                connector.ack(batchId); // 提交确认
            }

            System.out.println("empty too many times, exit");
        } finally {
            connector.disconnect();
        }
    }

    private static void printEntry(List<Entry> entrys) {
        for (Entry entry : entrys) {
            if (entry.getEntryType() == EntryType.TRANSACTIONBEGIN || entry.getEntryType() == EntryType.TRANSACTIONEND) {
                continue;
            }

            RowChange rowChage = null;
            try {
                rowChage = RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                throw new RuntimeException("ERROR ## parser of eromanga-event has an error , data:" + entry.toString(),
                        e);
            }

            EventType eventType = rowChage.getEventType();
            System.out.println(String.format("================&gt; binlog[%s:%s] , name[%s,%s] , eventType : %s",
                    entry.getHeader().getLogfileName(), entry.getHeader().getLogfileOffset(),
                    entry.getHeader().getSchemaName(), entry.getHeader().getTableName(),
                    eventType));

            for (RowData rowData : rowChage.getRowDatasList()) {
                if (eventType == EventType.DELETE) {
                    printColumn(rowData.getBeforeColumnsList());
                } else if (eventType == EventType.INSERT) {
                    printColumn(rowData.getAfterColumnsList());
                } else {
                    System.out.println("-------&gt; before");
                    printColumn(rowData.getBeforeColumnsList());
                    System.out.println("-------&gt; after");
                    printColumn(rowData.getAfterColumnsList());
                }
            }
        }
    }

    private static void printColumn(List<Column> columns) {
        for (Column column : columns) {
            System.out.println(column.getName() + " : " + column.getValue() + "    update=" + column.getUpdated());
        }
    }


}
```





## canal-spring-boot-starter

### quickstart

1.引入依赖

```xml
	  <dependency>
		  <groupId>com.git</groupId>
		  <artifactId>canal-spring-boot-starter</artifactId>
		  <version>0.0.1-SNAPSHOT</version>
	  </dependency>
```

2.配置yml文件

```yml
com:
  git:
    canal:
      username: canal
      password: canal
      destination: example
      filter: "open_md.o_user"
      hostname: 127.0.0.1
      sleep-time: 10000
```

```
配置说明
1.username就是canal服务端的账号，如果没有设置可不填，在instance.里面设置04-29日改
2.password就是canal服务端的密码，如果没有设置可不填
3.destination这个就是canal实例的名称，这个必须在canal服务端配置好，默认自带example
4.filter过滤规则，如果在客户端配置了这个，服务端的过滤配置会被覆盖，表示监听那些标，规则database.tableName
5.hostname canal服务端ip
6.port这里没有配置，默认11111，canal服务端的端口
7.sleep-time，线程休眠时间，暂时采用sleep策略，单位ms，相当于每隔这个时间消费一次数据
```

3.编写业务代码

```java
    @CanalListener(databaseName = "open_md",tableName = "o_user")
    public void consumerUser(User user, CanalEntry.EventType eventType){
        log.info("user={}，操作类型={}",JSON.toJSONString(user),eventType.name());
 
        boolean b = userRepository.existsById(user.getId());
        switch (eventType){
            case INSERT:
            case UPDATE:
                System.out.println("=========正在添加或者修改文档===========");
                userRepository.save(user);
                break;
            case DELETE:
                System.out.println("=========正在删除文档===========");
                if(b){
                    userRepository.delete(user);
                }
                break;
            default:
                System.out.println("=========操作类型不匹配===========");
                System.out.println(user);
        }
    }
```



```
上述代码是在一个正常的server里面的业务代码，该类有添加@Service注解，被spring管控
说明：
1.consumerUser是一个消费者，个人业务代码方法规则，方法返回参数不限，方法名不限，方法参数有限制，至少两个参数
2.方法第一个参数类型不限(推荐和数据库对应一致的po对象)，第二个参数类型固定为CanalEntry.EventType，这个参数代表数据发生了增删改的那些操作。
3.消费方法必须加@CanalListener注解，不然不会自动执行里面的代码
@CanalListener参数说明
	-> id每个实例的id，暂时可以不用配置
	-> databaseName,必填会根据这个决定业务方法执行那些库的操作
    -> tableName 必填数据库的表名，和上面结合一起决定监听个表
	-> handler 选填，如何业务代码第一个参数是和数据库对应一致（实体类必须符合驼峰命名规范）可不配，如果是自定义参数，必修配置handle，实现RowDataHandler接口，并且自定义handle必须被spring管控
```

4.启动项目，会看到业务代码的执行


### 项目源码介绍
1.项目依赖
```xml
<dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-collections4</artifactId>
            <version>4.1</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.47</version>
        </dependency>

        <dependency>
            <groupId>com.alibaba.otter</groupId>
            <artifactId>canal.client</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>org.springframework</groupId>
                    <artifactId>spring</artifactId>
                </exclusion>
            </exclusions>
            <version>1.1.4</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
    </dependencies>
```



2.项目的类说明

```java
CanalFactory canal连接类，实现了DisposableBean接口，会在生命周期结束的时候关掉连接
CanalListener CanalListener注解
CanalListenerConsumer 动态代理工具类
CanalMessageListener 消费接口，
CanalProperties 配置类，对应于yml里面的配置
CanalService 获取和消费canal数据的类
CanalSpringListener 事件派发，在项目启动之后派发消息启动CanalService里面onmessage方法
DefalutRowDataHandler 默认RowDataHandler实现类
RowDataHandler 数据转换接口
StringUtils 内部的工具类，不对外开放
```



核心类代码说明

```java
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

```

```
1.CanalService类上面的注解说明
	->@EnableConfigurationProperties({CanalProperties.class})开启配置自动注入，相当与导入yml的配置
	->@Import({CanalFactory.class, CanalSpringListener.class, DefalutRowDataHandler.class})
	手工引入这三个类被spring所管理，CanalService依赖于这三个类
2.实现接口说明
	->BeanPostProcessor bean的后置处理器用于收集那些方法加了@CanalListener注解并将这些方法收集到map和list里面
	->ApplicationListener 实现spring的监听器，用于接受自定义的派发事件
3.逻辑梳理
	1.项目启动，spring自动读取yml文件并映射为对应的配置类
	2.canalFactory会建立和canalServer的连接
	3.CanalService收集所有被@CanalListener修饰的方法
		->list收集方法
		->map会将这些方法通过CanalListenerConsumer工具类生成CanalMessageListener代理对象，
		map的key就是注解的databaseName+tableName，如果重复会被覆盖
	4.项目启动完 CanalSpringListener派发事件
	5.CanalService接收到消息，开始执行 process()方法
```



```
proceess()方法详解
1.创建线程
2.canal读取数据,得到message对象
3.canal消费数据
	->消费失败，尝试三次重新消费，超过失败放弃数据
	->消费过程
		->onMessage(message);
		首先获得List<CanalEntry.Entry>，遍历，CanalEntry.Entry相当于数据库的多条row对象
		根据Entry对象的databaseName和tableName决定哪个消费者来处理，执行CanalMessageListener里面的onMessage方法
	->消费成功会进行提交commit
```



```java
CanalListenerConsumer代理方法详解
    1.创建代理对象,根据注解和被注接修饰的方法
    public CanalListenerConsumer(Object bean, Method method,CanalListener canalListener) {
        this.bean = bean;
        this.method = method;
        this.canalListener = canalListener;
    }
	2.逻辑处理
       遍历CanalEntry.Entry，得到rowData，得到行记录，将行记录转化为po对象，使用RowDataHandle接口的方法，有一个默认的实现接口
    3.执业务方法，执行的就是被业务代码里面的方法
```

### 完善与发展

1.本文代码不仅完善之处也不多说

2.后续发展

 - 保留直接消费message的方法
 - 开启线程和线程休眠的改善

参考文章：[canal官网](https://github.com/alibaba/canal/wiki/AdminGuide)
代码已在github