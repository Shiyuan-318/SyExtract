# SyExtract

一款适用于 Minecraft Purpur 1.21.1 的红包插件，支持 Vault 经济系统。

## 功能特性

- 创建红包（设置名称、金额、数量）
- GUI界面查看和领取红包
- Vault经济系统支持
- 可配置的手续费系统
- 玩家封禁系统（限制发放/领取红包）
- 红包过期时间设置
- 高度可配置

## 安装要求

- Minecraft Server: Purpur 1.21.1
- Java: 21 或更高版本
- 依赖插件: Vault + 任意经济插件（如 EssentialsX）

## 命令列表

| 命令 | 权限 | 描述 |
|------|------|------|
| `/sye create <名称> <金额> <数量>` | syextract.create | 创建红包 |
| `/sye open` | syextract.open | 打开红包GUI |
| `/sye ban <玩家名> <时间>` | syextract.ban | 封禁玩家 |
| `/sye unban <玩家名>` | syextract.unban | 解封玩家 |
| `/sye reload` | syextract.reload | 重载配置 |

### 时间格式
封禁时间支持以下格式：
- `1h` - 1小时
- `1d` - 1天
- `1w` - 1周
- `30d` - 30天

## 权限节点

| 权限 | 默认 | 描述 |
|------|------|------|
| syextract.use | true | 基础使用权限 |
| syextract.create | true | 创建红包权限 |
| syextract.open | true | 打开GUI权限 |
| syextract.ban | op | 封禁玩家权限 |
| syextract.unban | op | 解封玩家权限 |
| syextract.reload | op | 重载配置权限 |
| syextract.admin | op | 所有管理员权限 |

## 配置文件

### config.yml

```yaml
# 红包设置
red-envelope:
  # 红包默认可领取时间（小时）
  default-expire-hours: 24
  # 最小红包金额
  min-amount: 1.0
  # 最小红包数量
  min-count: 1
  # 最大红包数量
  max-count: 100

# 手续费设置
fee:
  # 是否启用手续费
  enabled: true
  # 手续费比例 (0.0 - 1.0, 例如 0.05 = 5%)
  percentage: 0.05
  # 最低手续费
  min-fee: 1.0

# GUI设置
gui:
  # GUI标题
  title: "&c&l红包大厅"
  # 红包物品材质
  item-material: "SUNFLOWER"
  # 红包物品显示名称
  item-name: "&6&l{sender} &e的红包"
```


## 数据存储

插件会在 `plugins/SyExtract/` 目录下生成以下数据文件：
- `envelopes.yml` - 红包数据
- `bans.yml` - 封禁数据

## 使用示例

1. **创建红包**
   ```
   /sye create 新年快乐 1000 10
   ```
   创建一个名为"新年快乐"的红包，总金额1000金币，分成10个。

2. **打开GUI领取红包**
   ```
   /sye open
   ```

3. **封禁玩家**
   ```
   /sye ban PlayerName 7d
   ```
   封禁玩家7天，禁止发放和领取红包。

4. **解封玩家**
   ```
   /sye unban PlayerName
   ```

## 注意事项

1. 创建红包时会扣除红包金额 + 手续费
2. 红包采用随机分配算法，每个红包金额随机
3. 过期未领取的红包将自动清理
4. 每个玩家只能领取同一个红包一次
