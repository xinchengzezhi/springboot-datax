![Java](https://woolson.gitee.io/npmer-badge/Java-555555-1.8-44cc11-check-ffffff-square-gradient-shadow.svg)
![springboot](https://woolson.gitee.io/npmer-badge/springboot-555555-2.x-44cc11-check-ffffff-square-gradient-shadow.svg)
## preparation
- Language: Java 8
- Environment: MacOS, 16G RAM
- Database: Mysql5.7

## introduction
- 使用springboot启动datax，不再需要用python启动。
- 以restful接口启动datax作业

## todo list

* [x] springboot重构项目
* [x] 通过restful接口调度datax完成抽取数据作业
* [x] 通过restful接口传入job配置json生成临时文件，根据文件配置调度datax执行该作业
* [x] 集成swagger，方便调试
* [x] 集成mybatis plus和Mysql数据库存放应用数据
* [x] 网页端修改并持久化job配置的json到数据库
* [x] 网页端实时查看抽取日志，类似Jenkins的日志控制台输出功能
* [ ] 网页端各种读写插件模板生成，可以在页面组装使用
* [ ] 精简assembly打包结构
* [ ] 实现datax分布式作业
* [ ] 实现部分写插件支持自动建表功能


## 前端项目
[github地址](https://github.com/zhouhongfa/datax-vue-admin.git)
## how to run
数据库脚本还是要执行的，如果使用web端的话，脚本：/Applications/myMac/zhongjian/code3/datax/DataX-SpringBoot-master/datax-web/db/datax_web.sql
### 1. 在父工程目录下使用maven打包，直接跳过此步，已经将插件生成好，放在resource目录下了，直接启动项目即可
```
 mvn -U clean package assembly:assembly -Dmaven.test.skip=true 
```

### 2. 在打包完成的target目录下进入datax-web，可以看到datax-web-0.0.1-SNAPSHOT
```
cd  datax/datax/plugin/web
```

### 3. 运行启动命令
```
 java  -Ddatax.home=/Users/huzekang/openSource/DataX/target/datax/datax  -jar datax-web-0.0.1-SNAPSHOT.jar
```
需要配上环境变量-Ddatax.home，此处参照上述配置mvn打包后的目录即可

### 4. 终端访问测试作业接口
```
curl http://localhost:8080/startJob
```
可以看到成功跑完一个datax作业
![](https://raw.githubusercontent.com/peter1040080742/picbed/master/20190505162333.png)

### 5. 打开网页端启动作业
http://localhost:8080/index.html#/datax/job
![](https://raw.githubusercontent.com/huzekang/picbed/master/20190617120207.png)

### 6. 在线查看作业日志
![](https://raw.githubusercontent.com/huzekang/picbed/master/20190708102445.png)
### 7. postman 调用 示例curl
```
curl --location 'http://localhost:8066/api/testStartJobJson' \
--header 'Accept: application/json, text/plain, */*' \
--header 'Accept-Language: zh-CN,zh;q=0.9' \
--header 'Connection: keep-alive' \
--header 'Content-Type: application/json;charset=UTF-8' \
--header 'Cookie: Admin-Token=admin-token' \
--header 'Origin: http://localhost:8066' \
--header 'Referer: http://localhost:8066/index.html' \
--header 'Sec-Fetch-Dest: empty' \
--header 'Sec-Fetch-Mode: cors' \
--header 'Sec-Fetch-Site: same-origin' \
--header 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36' \
--header 'X-Token: admin-token' \
--header 'sec-ch-ua: "Chromium";v="128", "Not;A=Brand";v="24", "Google Chrome";v="128"' \
--header 'sec-ch-ua-mobile: ?0' \
--header 'sec-ch-ua-platform: "macOS"' \
--data-raw '{
    "jobConfigId": 7,
    "jobJson": "{\n  \"job\": {\n    \"setting\": {\n      \"speed\": {\n        \"channel\": 5\n      }\n    },\n    \"content\": [\n      {\n        \"reader\": {\n          \"name\": \"mysqlreader\",\n          \"parameter\": {\n            \"username\": \"app_owner\",\n            \"password\": \"YZ.zk.owner@2016\",\n            \"column\": [\n              \"name\",\n              \"tenant_id\",\n              \"code\"\n            ],\n            \"where\": \"(CAST(substring(code, 5, 19) AS UNSIGNED) ^((CAST(substring(code, 5, 19) AS UNSIGNED) >> 2))) & 2 = 0\",\n            \"connection\": [\n              {\n                \"table\": [\n                  \"test\"\n                ],\n                \"jdbcUrl\": [\n                  \"jdbc:mysql://olrating-rw-qa.yzw.cn:6302/jc_ppls\"\n                ]\n              }\n            ]\n          }\n        },\n        \"writer\": {\n          \"name\": \"mysqlwriter\",\n          \"parameter\": {\n            \"username\": \"app_owner\",\n            \"password\": \"YZ.zk.owner@2016\",\n            \"column\": [\n              \"name\",\n              \"tenant_id\",\n              \"code\"\n            ],\n            \"preSql\": [],\n            \"postSql\": [],\n            \"connection\": [\n              {\n                \"table\": [\n                  \"test_user_0\"\n                ],\n                \"jdbcUrl\": \"jdbc:mysql://olrating-rw-qa.yzw.cn:6302/jc_ppls\"\n              }\n            ]\n          }\n        }\n      },\n      {\n        \"reader\": {\n          \"name\": \"mysqlreader\",\n          \"parameter\": {\n            \"username\": \"app_owner\",\n            \"password\": \"YZ.zk.owner@2016\",\n            \"column\": [\n              \"name\",\n              \"tenant_id\",\n              \"code\"\n            ],\n            \"where\": \"(CAST(substring(code, 5, 19) AS UNSIGNED) ^((CAST(substring(code, 5, 19) AS UNSIGNED) >> 2))) & 2 = 1\",\n            \"connection\": [\n              {\n                \"table\": [\n                  \"test\"\n                ],\n                \"jdbcUrl\": [\n                  \"jdbc:mysql://olrating-rw-qa.yzw.cn:6302/jc_ppls\"\n                ]\n              }\n            ]\n          }\n        },\n        \"writer\": {\n          \"name\": \"mysqlwriter\",\n          \"parameter\": {\n            \"username\": \"app_owner\",\n            \"password\": \"YZ.zk.owner@2016\",\n            \"column\": [\n              \"name\",\n              \"tenant_id\",\n              \"code\"\n            ],\n            \"preSql\": [],\n            \"postSql\": [],\n            \"connection\": [\n              {\n                \"table\": [\n                  \"test_user_1\"\n                ],\n                \"jdbcUrl\": \"jdbc:mysql://olrating-rw-qa.yzw.cn:6302/jc_ppls\"\n              }\n            ]\n          }\n        }\n      }\n    ]\n  }\n}"
}'
```
### 8.不启动项目，直接执行脚本
解压 yzw-datax.zip包
在这个路径下：DataX-SpringBoot-master/datax-web/src/main/resources/target/datax/datax/bin
执行脚本：python datax.py  /xx/xx.json
