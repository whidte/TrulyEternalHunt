# libs

编译期需要以下 jar（不会被提交到 git）：

| 文件 | 来源 |
| --- | --- |
| `trulybestfriends-0.2.6-1.21.1.jar` | 在 `../trulybestfriends`（1.21.1 分支）执行 `gradlew build`，从 `build/libs/` 复制 |
| `enigmaticlegacyplus-1.21.1-1.0.0.jar` | [Enigmatic Legacy+ 1.0.0 on Modrinth](https://modrinth.com/mod/enigmaticlegacy-plus) |
| `curios-neoforge-9.5.1+1.21.1.jar` | [Curios 9.5.1 on Modrinth](https://modrinth.com/mod/curios) |
| `relics-1.21.1-0.12.10.jar` | [Relics 0.12.10 on Modrinth](https://modrinth.com/mod/relics-mod) |
| `OctoLib-NEOFORGE-0.6.2+1.21.jar` | [OctoLib 0.6.2 on Modrinth](https://modrinth.com/mod/shatterbyte-lib)（Relics 的前置，编译期需要） |
| `dream_relics-1.0.5-neoforge+mc1.21.1.jar` | [Dream Relics 1.0.5 on Modrinth](https://modrinth.com/mod/dream-relics) |

也可以直接运行 `scripts/setup-libs.ps1`，脚本会从本机 TBF 构建目录复制，并从 Modrinth 下载 ELP、Curios、Relics、OctoLib 与 Dream Relics。
