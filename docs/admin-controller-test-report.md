# Controller 与 Service 测试报告

## 1. 总览

- 项目：`SoftwareTestingDemo`
- 文档更新时间：`2026-04-21`
- Controller 层测试类型：集成测试
- Controller 命名规范：`*IT.java`
- Controller 执行插件：`maven-failsafe-plugin`
- Service 层测试类型：单元测试
- Service 命名规范：`*Test.java`
- Service 执行插件：`maven-surefire-plugin`

本轮测试已经覆盖两层：

- `src/main/java/com/demo/controller`
- `src/main/java/com/demo/service/impl`

当前验证结果：

- `mvn test`：65 个单元测试，全部通过
- `mvn verify`：115 个集成测试，全部通过

## 2. 测试脚本清单

### 2.1 Controller 集成测试

| 模块 | Controller | 测试脚本 | 用例数 |
| --- | --- | --- | ---: |
| 首页 | `IndexController` | `src/test/java/com/demo/controller/IndexControllerIT.java` | 3 |
| User | `MessageController` | `src/test/java/com/demo/controller/user/MessageControllerIT.java` | 11 |
| User | `NewsController` | `src/test/java/com/demo/controller/user/NewsControllerIT.java` | 7 |
| User | `OrderController` | `src/test/java/com/demo/controller/user/OrderControllerIT.java` | 19 |
| User | `UserController` | `src/test/java/com/demo/controller/user/UserControllerIT.java` | 16 |
| User | `VenueController` | `src/test/java/com/demo/controller/user/VenueControllerIT.java` | 7 |
| Admin | `AdminMessageController` | `src/test/java/com/demo/controller/admin/AdminMessageControllerIT.java` | 8 |
| Admin | `AdminNewsController` | `src/test/java/com/demo/controller/admin/AdminNewsControllerIT.java` | 10 |
| Admin | `AdminOrderController` | `src/test/java/com/demo/controller/admin/AdminOrderControllerIT.java` | 7 |
| Admin | `AdminUserController` | `src/test/java/com/demo/controller/admin/AdminUserControllerIT.java` | 13 |
| Admin | `AdminVenueController` | `src/test/java/com/demo/controller/admin/AdminVenueControllerIT.java` | 14 |

### 2.2 Service 单元测试

| 模块 | Service | 测试脚本 | 用例数 |
| --- | --- | --- | ---: |
| Service | `MessageServiceImpl` | `src/test/java/com/demo/service/impl/MessageServiceImplTest.java` | 11 |
| Service | `MessageVoServiceImpl` | `src/test/java/com/demo/service/impl/MessageVoServiceImplTest.java` | 5 |
| Service | `NewsServiceImpl` | `src/test/java/com/demo/service/impl/NewsServiceImplTest.java` | 5 |
| Service | `OrderServiceImpl` | `src/test/java/com/demo/service/impl/OrderServiceImplTest.java` | 18 |
| Service | `OrderVoServiceImpl` | `src/test/java/com/demo/service/impl/OrderVoServiceImplTest.java` | 5 |
| Service | `UserServiceImpl` | `src/test/java/com/demo/service/impl/UserServiceImplTest.java` | 8 |
| Service | `VenueServiceImpl` | `src/test/java/com/demo/service/impl/VenueServiceImplTest.java` | 8 |
| Utility | `FileUtil` | `src/test/java/com/demo/utils/FileUtilTest.java` | 4 |

## 3. Controller 集成测试覆盖摘要

### 3.1 IndexController

覆盖重点：

- 首页是否正常返回
- 新闻、场馆首屏数据是否能从真实数据库装载
- 空数据时页面是否仍能正常渲染

### 3.2 User 侧 Controller

#### MessageController

覆盖重点：

- 留言页与留言列表
- 默认分页与非法分页
- 新增留言、删除留言
- 未登录访问的异常分支

#### NewsController

覆盖重点：

- 新闻详情页
- 新闻列表分页接口
- 默认分页、非法分页、缺少 `newsID`
- 空列表页面渲染

#### OrderController

覆盖重点：

- 订单管理页、订单创建页、订单修改页
- 下单、改单、完成订单、删除订单
- 按日期查询场馆订单
- 默认分页、非法分页、缺少 `venueID/orderID`
- 未登录下的访问与提交异常

#### UserController

覆盖重点：

- 登录、退出登录、注册
- 管理员/普通用户登录分支
- 错误密码、用户不存在、异常 `isadmin` 值
- 用户信息修改、头像上传、空密码不改密码
- `checkPassword` true/false

#### VenueController

覆盖重点：

- 场馆详情页
- 场馆列表页和分页接口
- 默认分页、非法分页、缺少 `venueID`
- 空列表页面渲染

### 3.3 Admin 侧 Controller

#### AdminMessageController

覆盖重点：

- 留言管理页
- 待审核留言分页接口
- 默认分页、非法分页、缺少 `messageID`
- 审核通过、驳回、删除后的数据库状态变化

#### AdminNewsController

覆盖重点：

- 新闻管理页、新增页、编辑页
- 新闻分页接口
- 默认分页、非法分页、缺少 `newsID`
- 新增、修改、删除后的数据库变化

#### AdminOrderController

覆盖重点：

- 预约审核页
- 待审核订单分页接口
- 默认分页、非法分页、缺少 `orderID`
- 审核通过、驳回后的订单状态变化

#### AdminUserController

覆盖重点：

- 用户管理页、新增页、编辑页
- 用户分页接口
- 默认分页、非法分页、缺少 `id`
- 新增、修改、删除用户
- 用户 ID 唯一性校验的正反分支

#### AdminVenueController

覆盖重点：

- 场馆管理页、新增页、编辑页
- 场馆分页接口
- 默认分页、非法分页、缺少 `venueID`
- 新增场馆、修改场馆、删除场馆
- 上传图片与不上传图片两类分支
- 场馆名唯一性校验的正反分支

## 4. Service 单元测试覆盖摘要

### 4.1 MessageServiceImpl

覆盖重点：

- 创建留言
- 查询分页留言
- 审核通过、驳回、删除
- 留言不存在时的异常分支

### 4.2 MessageVoServiceImpl

覆盖重点：

- 留言 VO 组装
- 空列表输入
- 关联用户为空时的当前行为

### 4.3 NewsServiceImpl

覆盖重点：

- 新增、删除、修改新闻
- 分页查询与详情查询

### 4.4 OrderServiceImpl

覆盖重点：

- 创建订单、修改订单、删除订单
- 查询用户订单、查询待审核订单、按日期查订单
- 订单审核通过、驳回、完成
- 订单不存在、场馆不存在时的异常/空对象路径

### 4.5 OrderVoServiceImpl

覆盖重点：

- 订单 VO 组装
- 空列表输入
- 关联用户或场馆为空时的当前行为

### 4.6 UserServiceImpl

覆盖重点：

- 用户新增、删除、修改
- 用户查询、登录相关查询
- 普通用户分页与计数

### 4.7 VenueServiceImpl

覆盖重点：

- 场馆新增、删除、修改
- 场馆查询、分页查询、名称统计

### 4.8 FileUtil

覆盖重点：

- 空文件上传返回空字符串
- 用户/场馆图片保存后路径格式正确
- 文件后缀保留正确

## 5. 执行命令

执行单元测试：

```powershell
mvn test
```

执行包含集成测试的完整验证：

```powershell
mvn verify
```

只执行 Controller 集成测试：

```powershell
mvn -q "-Dit.test=com.demo.controller.IndexControllerIT,com.demo.controller.user.MessageControllerIT,com.demo.controller.user.NewsControllerIT,com.demo.controller.user.OrderControllerIT,com.demo.controller.user.UserControllerIT,com.demo.controller.user.VenueControllerIT,com.demo.controller.admin.AdminMessageControllerIT,com.demo.controller.admin.AdminNewsControllerIT,com.demo.controller.admin.AdminOrderControllerIT,com.demo.controller.admin.AdminUserControllerIT,com.demo.controller.admin.AdminVenueControllerIT" verify
```

## 6. 结论

当前项目已经形成分层测试方案：

- Controller 层统一使用真实 Spring Boot 集成测试
- Service 层使用隔离式单元测试
- 工具类补充独立单元测试

这使得我们既能验证：

- 接口在真实容器、真实 DAO、测试数据库中的集成行为

也能验证：

- Service 业务逻辑与异常分支的细粒度行为

因此，当前文档口径不再仅限于 Admin，而是已经覆盖首页、User、Admin 和 Service 层的整体测试情况。
