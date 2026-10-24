# libs

编译期需要以下 jar（不会被提交到 git）：

| 文件 | 来源 |
| --- | --- |
| `trulybestfriends-0.2.5.jar` | 在 `../trulybestfriends-master` 执行 `gradlew build`，从 `build/libs/` 复制 |
| `EnigmaticLegacy-2.30.1.jar` | Enigmatic Legacy 2.30.1（CurseForge） |
| `enigmaticaddons-1.2.6.jar` | Enigmatic Addons 1.2.6（CurseForge / Modrinth） |
| `curios-forge-5.14.1+1.20.1.jar` | Curios 5.14.1（CurseForge / Modrinth） |

也可以直接运行 `scripts/setup-libs.ps1`，脚本会从本机常见的整合包目录复制这些 jar。
