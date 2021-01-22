# pgwal2mqtt

捕获Postgres的数据变化，并将其转发到MQTT服务（全流程异步高性能）

目前发送的消息是可以根据配置提供`详细`/`汇总`两种模式，

* 详细模式例子如下：

```json
[
  {
    "schema": "platform",
    "kind": "update",
    "oldkeys": {
      "keynames": [
        "id"
      ],
      "keyvalues": [
        "1"
      ],
      "keytypes": [
        "character varying"
      ]
    },
    "columntypes": [
      "character varying",
      "character varying",
      "character varying",
      "timestamp without time zone",
      "timestamp without time zone",
      "timestamp without time zone",
      "timestamp without time zone",
      "boolean"
    ],
    "columnvalues": [
      "1",
      "admin",
      "62O9OPcew32TeECPtmqU9SQ==",
      "2019-06-12 08:50:33.051123",
      "2020-10-23 17:11:35.786411",
      "2021-01-22 16:51:56.591169",
      "2021-01-22 16:53:26.964909",
      true
    ],
    "columnnames": [
      "id",
      "v_username",
      "v_password",
      "t_create",
      "t_update",
      "t_lastlogin",
      "t_thislogin",
      "b_enable"
    ],
    "table": "auth_user"
  }
]
```

* 汇总模式例子如下:

```json
{
  "schema": "platform",
  "table": "auth_user",
  "type": "update",
  "num": 1
}
```

### 原理

因为Postgres的事务日志是记录了每个数据库的微小变更的，所以只要解析该日志，并把它的数据推送到mqtt服务即可

### 巨人的肩膀

1. [wal2json](https://github.com/eulerto/wal2json) ，用来解析Postgres wal日志成为JSON格式。
2. [NuProcess](https://github.com/brettwooldridge/NuProcess) ，非阻塞方式执行Java外部进程（shell命令），可替代Java的java.lang.Process
3. [childprocess-vertx-ext](https://github.com/vietj/childprocess-vertx-ext) ，融合进Vert.x生态的NuProcess，开箱即用

### 使用步骤

1. 修改Postgres数据库的配置文件postgresql.conf

    ```shell
    wal_level = logical
    max_replication_slots = 10
    max_wal_senders = 10
    ```
   更改配置后，需要重启Postgres服务

2. 在Postgres数据库服务器上安装`wal2json13`

    ```shell
    sudo apt-get install postgresql-13-wal2json
    ```

3. 在需要运行pgwal2mqtt的电脑上安装Java环境

   ```shell
   sudo apt install openjdk-11-jre
   ```

4. 准备pgwal2mqtt的配置文件(config.json)，存放在硬盘某处，下面是参考

   ```json
   {
   "dbs": [
         {
            "unique": "topicA",
            "slot": "slot1",
            "summary": false,
            "host": "127.0.0.1",
            "port": 5432,
            "database": "studypg",
            "username": "postgres",
            "exclude": [
              "log.*",
              "log.log_access",
              "*.log_access"
            ]
         },
         {
            "unique": "topicB",
            "host": "127.0.0.1",
            "port": 5433,
            "database": "postgres",
            "username": "liurui",
            "include": [
              "log.*"
            ]
         }
      ],
   "mqtt": 
      {
         "host": "192.168.0.88",
         "port": 31883
      }
   }
   ```

   dbs是一个Array，可以填写多个数据库信息，需要注意的是**unique**是该数据库的代号，也是mqtt消息的**topic**一定要保证全局唯一。
   另外你可能注意到，配置里面没有出现password，因为Postgres的安全性限制不允许出现明文密码，所以你需要使用`.pgpass`
   声明各个数据库的密码，具体可以参考文档[pgpass](http://postgres.cn/docs/13/libpq-pgpass.html)

   `slot`名称可选填，不填默认`pgwal2mqtt_slot`

5. 通过以下命令运行pgwal2mqtt

   ```shell
   java -jar pgwal2mqtt-1.0-fat.jar -conf config.json
   ```

   如果有问题，上面的.jar、.json文件均可替换成绝对路径 测试运行成功后，也可以把本项目当作服务启动，以实现在后台静默运行

   ```shell
   java -jar pgwal2mqtt-1.0-fat.jar start -conf config.json
   ```

### 更新日志

* 1.0.3 支持根据配置，确定消息采用明细模式还是汇总模式
* 1.0.2 支持根据配置排除某些表，或者仅包含某些表（排除优先级更高）
* 1.0.1 增加了对mqtt服务的断线重连功能
