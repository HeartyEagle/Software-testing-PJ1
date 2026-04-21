# Controller 集成测试与 Service 单元测试设计说明

## 1. 设计目标

本说明用于介绍当前项目测试设计的整体思路。为了同时满足课程中对 Controller 集成测试的要求，以及对业务逻辑正确性的验证需求，项目采用了分层测试方案：

- Controller 层使用集成测试
- Service 层使用单元测试
- Utility 层补充独立单元测试

这样做的目的，是把“真实链路验证”和“局部逻辑验证”分开处理：

- 集成测试负责验证请求进入系统后，Controller、Service、DAO 和测试数据库之间是否能正确协作
- 单元测试负责验证 service 实现中的业务分支、异常路径和边界输入

## 2. 当前测试范围

### 2.1 Controller 层

当前已经覆盖全部公开 Controller：

- `IndexController`
- `MessageController`
- `NewsController`
- `OrderController`
- `UserController`
- `VenueController`
- `AdminMessageController`
- `AdminNewsController`
- `AdminOrderController`
- `AdminUserController`
- `AdminVenueController`

对应测试文件统一位于 `src/test/java/com/demo/controller` 下，并采用 `*IT.java` 命名。

### 2.2 Service 层

当前已经覆盖全部 `service.impl` 实现类：

- `MessageServiceImpl`
- `MessageVoServiceImpl`
- `NewsServiceImpl`
- `OrderServiceImpl`
- `OrderVoServiceImpl`
- `UserServiceImpl`
- `VenueServiceImpl`

对应测试文件位于 `src/test/java/com/demo/service/impl` 下，并采用 `*Test.java` 命名。

### 2.3 Utility 层

当前已补充：

- `FileUtilTest`

用于补足文件上传保存逻辑的独立验证。

## 3. Maven 执行策略

项目采用 Maven 的标准测试分工：

- `maven-surefire-plugin` 执行单元测试
- `maven-failsafe-plugin` 执行集成测试

因此：

- `mvn test` 运行 `*Test.java`
- `mvn verify` 在完整验证阶段运行 `*IT.java`

这种设计的好处是：

- 单元测试运行更快，适合日常回归
- 集成测试更贴近真实系统运行方式，适合做阶段性验证和展示

## 4. Controller 集成测试设计思路

### 4.1 为什么 Controller 使用集成测试

Controller 测试的目标不是只看某个方法有没有调用 mock service，而是验证一组完整前端请求在系统集成后是否能被正确处理。

因此当前 Controller 测试统一采用：

- `@SpringBootTest`
- `@AutoConfigureMockMvc`
- `@ActiveProfiles("test")`

并配合：

- `application-test.yml`
- H2 内存数据库
- 真实 DAO 持久化操作

这样每条 Controller 用例都能覆盖：

1. 请求映射是否正确
2. 参数绑定是否正确
3. 视图名或 JSON 返回是否正确
4. Service 与 DAO 是否成功协作
5. 数据库状态是否真实变化

### 4.2 Controller 用例选择原则

每个 Controller 都尽量从下面几类中选取用例：

- 页面渲染类
- 分页与列表类
- 新增/修改/删除类
- 状态迁移类
- 唯一性校验类
- 登录保护类
- 默认参数类
- 非法参数类
- 文件上传类
- 空数据类

### 4.3 断言策略

每条 Controller 集成测试尽量包含两类断言：

- 接口行为断言
  - 例如状态码、视图名、JSON 字段、重定向地址
- 持久化结果断言
  - 例如数据库新增、更新、删除、状态变化

这能避免测试只停留在“返回值对了”，而忽略真实系统状态是否一致。

## 5. Service 单元测试设计思路

### 5.1 为什么 Service 使用单元测试

Service 层更适合用 mock DAO 的方式做细粒度验证，因为这里最需要的是：

- 打到更多分支
- 模拟异常输入
- 快速定位业务逻辑错误

相比集成测试，单元测试更容易覆盖：

- 对象不存在
- DAO 返回空对象
- 空列表输入
- 特定分支下的状态变化

### 5.2 Service 用例选择原则

对每个 Service 实现类，优先覆盖：

- 正常业务路径
- 关键状态流转
- 显式异常分支
- 空集合边界
- 依赖对象为空时的当前行为

其中：

- `OrderServiceImpl`
- `MessageServiceImpl`
- `OrderVoServiceImpl`
- `MessageVoServiceImpl`

是分支和边界较多的重点对象，因此单元测试也更密集。

## 6. 数据准备与复用

为了让集成测试更稳定、可重复，Controller 测试使用了统一的基类：

- `AbstractControllerIT`
- `AbstractAdminControllerIT`

基类负责：

- 注入 `MockMvc`
- 注入真实 DAO
- 提供 `cleanDatabase()` 清理逻辑
- 提供创建用户、场馆、新闻、订单、留言等辅助方法

这样每个测试类只需要专注于：

- 构造本用例需要的数据
- 发起请求
- 断言结果

## 7. 当前覆盖重点

### 7.1 已重点覆盖的情况

- 所有公开 Controller 的主路径
- 默认分页参数
- 非法分页参数 `page=0`
- 缺少关键 primitive 参数
- 登录保护分支
- 用户和场馆唯一性校验正反分支
- 文件上传与不上传的双分支
- 审核通过、驳回、完成等状态迁移
- 空数据页面渲染
- VO 组装的空列表情况
- `FileUtil` 的核心保存逻辑

### 7.2 为什么这样设计更适合拿分

这种设计相对单纯的 mock controller 测试，有几个明显优势：

- 能说明 Controller 测试是真实集成测试，而不是伪集成
- 能展示系统分层测试设计，而不是只堆用例数量
- 能同时体现正常路径、边界情况和异常分支
- 文档和代码结构与 Maven 主流规范一致

## 8. 运行方式

运行单元测试：

```powershell
mvn test
```

运行完整验证：

```powershell
mvn verify
```

## 9. 边界说明

当前方案已经明显强于只验证 happy path 的测试设计，但如果把目标定义为“理论上所有可能情况完全穷尽”，那么仍然可以继续扩展：

- 更细的非法参数组合
- 对象不存在时的全部 Controller 行为
- DAO 主动抛异常时的传播行为
- 文件系统写入失败场景
- 浏览器级端到端测试

因此，当前口径更适合表述为：

“项目已完成覆盖首页、User、Admin 的 Controller 集成测试，以及 Service 层和关键 Utility 的单元测试；主要业务路径、关键分支、边界输入与部分异常路径已得到验证。”
