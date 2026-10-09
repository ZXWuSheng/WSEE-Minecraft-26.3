# 二段跳与飞钩隔离验证

本目录提供真实服务端、TCP 协议客户端和测试驱动的原版移动验证。测试在独立世界中运行，不启动 Minecraft 客户端窗口，也不替代两端人工游戏验证。QA 源集只参与隔离运行，不打入发布用 WSEE JAR。

## 修改文件职责

| 文件或文件组 | 职责 |
| --- | --- |
| `DoubleJumpController.java`、`WseeMod.java` | 一次落地周期的跳跃机会、服务端资格检查及一次向上增量 |
| `MovementMath.java`、`FlyingHookController.java`、`FlyingHookCycle.java` | 绳长与拉力计算、真实锚点、阶段门闩、装备归属及清理 |
| `ServerMotion.java`、`MovementImpulsePayload.java`、`MovementImpulseState.java` | 服务端授权增量、实际运动记录、会话序号去重及本次新增力限速 |
| `ClientHooks.java`、`ModNetworking.java`／Fabric 注册入口 | 绑定跳跃键请求、加载器网络注册与客户端主线程收包 |
| `FlyingHookUsePayload.java`、`FlyingHookInputMixin.java`、`FlyingHookUseKeyMixin.java`、`FishingRodUseMixin.java` | 使用键上升沿、钩头 UUID／阶段校验及原始主副手 |
| `FlyingHookStateMixin.java`、`MovementAttachments.java`（NeoForge） | 钩头阶段与持竿手同步，替代普通鱼漂运动和取鱼流程 |
| `FlyingHookAnchorPayload.java`、`FlyingHookClientState.java`、`FishingHookRendererMixin.java` | 精确锚点、实体偏移、钩头与绳索端点表现 |
| `flying_hook.json`、附魔数值／注册、语言 JSON、混入配置、两端 README／AGENTS 与规格 DOCX | 飞钩 I 级、原版距离、玩法描述、资源与规范一致性 |
| 运动回归类、静态检查脚本及本目录 | 计算／协议回归、资源检查、真实服务端双人测试与中文证据汇总 |

## 当前操作

二段跳靴子在真实落地后提供一次空中机会。松开并再次按下绑定的跳跃键，服务端验证资格后授权一次 `(0, 0.65, 0)` 增量。客户端收到授权才向自身实际当前速度叠加；长按和同一机会的重复请求不能增加次数。既有坠落距离重置及摔落起算高度增加两格规则继续保留。

飞钩只有 I 级，最大距离与原版钓鱼竿相同。首次使用发射；命中有效碰撞表面或活体实体后固定等待；再次使用开始持续收绳；收绳时再次使用取消并回收；尚未命中时再次使用回收；到达锚点附近自动回收。最初发射的手和鱼竿拥有该钩头。收绳途中可二段跳，取消飞钩不会刷新跳跃次数。

服务端控制命中、锚点、阶段、资格和拉力计算，使用已接受的客户端运动记录估算径向速度。它只发送本次授权增量，客户端按当前自身、维度、会话 UUID 和递增序号校验后去重叠加。不得用服务端全量速度替换客户端惯性，也不得把输入预测当作服务端授权。正向牵引包携带服务端径向上限；收包端只保守缩小本次授权增量，不裁剪已有速度或切向运动。协议客户端复用生产代码的 `MovementImpulseState.approvedDelta`，以当前镜像速度执行相同限速规则。

## 运行与证据

游戏与编译使用 Java 25。NeoForge 在本机使用 Java 21 启动 Gradle，由工具链选择 Java 25；Fabric 的 Gradle 使用 Java 25。分别在相应项目根目录运行已有包装器：

```powershell
# NeoForge
.\gradlew.bat clean check build --offline --no-daemon
.\gradlew.bat -I scripts/server-validation/neoforge.init.gradle runMovementValidation --offline --no-daemon

# Fabric（在 Fabric 项目根目录运行）
.\gradlew.bat clean check build --offline --no-daemon
.\gradlew.bat -I scripts/server-validation/fabric.init.gradle runMovementQaServer --offline --no-daemon
```

NeoForge 与 Fabric 分别监听 `127.0.0.1:25165` 和 `127.0.0.1:25166`，仅供本地隔离验证。初始化脚本使用已有的本地测试 EULA 文件，不自动代替用户接受协议。测试目录分别为 `build/qa-neoforge` 和 `build/qa-fabric`；其中的测试世界与日志均为生成文件，不应提交。Fabric 的测试源集复用相邻 NeoForge 项目中本目录的公共驱动。

两端完成当前运行后，在 NeoForge 项目执行 `python scripts/server-validation/analyze_trace.py`，生成 `build/reports/运动运行验收.md`。该脚本需要两个当前结果都为零失败且九条轨迹均正常到达，否则返回非零。

检查本次运行生成的 `movement-qa-result.txt`、`movement-qa-trace.jsonl` 和服务端日志。必须同时确认断言无失败、结果属于当前源码运行、连接未异常断开，不能仅凭 Gradle 的完成提示或旧结果文件判断通过。轨迹记录包含运动向量、碰撞、锚点间距、阶段和结束原因；实际断言数、最大径向速度与到达情况由最终运行结果记录。

| 验证方式 | 可以确认 | 仍需其他验证 |
| --- | --- | --- |
| Python 静态检查与缓存 API 核对 | 两端接线、资源、协议、方法及注入目标 | 实际网络处理、碰撞及游戏手感 |
| 共享 Java 回归 | 冲量计算、输入机会、阶段门闩、序号与会话去重 | 玩家真实登录和客户端表现 |
| TCP 协议客户端与真实 ServerPlayer | 登录、编解码、服务端资格、请求隔离、授权包收发 | 完整 Minecraft 客户端输入、主线程处理和渲染 |
| 测试驱动的原版 travel | 原版碰撞路径、轨迹、到达、有限加速及清理 | 正常客户端持续输入、真实网络延迟及操作手感 |
| 两端人工游戏验证 | 真实输入、绳索显示、多人观察、手感与模组交互 | 需分别记录 Fabric 与 NeoForge 结果 |

协议客户端不会渲染钩头或绳索。它使用共享收包门闩和速度镜像；这不等同于执行完整客户端的 ClientHooks。轨迹验证会把镜像位置和速度交给原版 `travel(Vec3.ZERO)`，保存并恢复服务端状态后通过 TCP 提交位置。该流程明确属于测试驱动的无界面物理，不能宣称已验证玩家客户端的自动移动与视觉效果。

## 人工验收

在两个加载器中分别检查静止、疾跑、上升、下落、快速下落和受外力时的二段跳，确认 X/Z 保留且 Y 只增加一次；检查长按、快速松开重按和重复请求。

检查墙面、台阶与半砖锚点，确认命中先固定、收绳才牵引；在近处和原版距离范围内分别测试正面、斜向及向上的收绳，确认不会穿墙、无限加速或反复越过锚点。收绳途中二段跳应保留到后续自然物理更新。

检查取消、到达、锚点破坏、失去鱼竿、钩头移除、死亡、退出和切换维度后的清理；检查主副手、普通钓鱼、空军、已有外部推力及另一名玩家观察到的钩头和绳索。真实客户端网络延迟与渲染尚须人工验证，构建和隔离测试通过不能替代这些结论。
