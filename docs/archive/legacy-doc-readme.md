# NextThing Documentation

> 文档基线：2026-08-12。当前项目存在大量尚未提交的开发版代码；功能与界面现状应优先以 `app/src/main/` 为准。README 和本目录用于解释当前实现，旧 HTML 原型仅供设计演进参考。

This directory keeps the project materials that are useful for implementation, visual review, and interview defense.

## Maintained In Git

- `UI设计/`: UI 规范、页面地图和历史 HTML 视觉稿。`00_设计总览.md` 维护当前设计基线；页面级旧稿可能落后于 Compose 实现，不再作为唯一事实来源。
- `ui-design-preview.html`: legacy design preview entry.
- `面试典型问题/`: project-specific interview questions and answer material.

## Not Maintained Here By Default

- Raw chat logs, long development transcripts, and temporary generated files.
- Large model files, binaries, APKs, or screenshots that are only one-off debugging evidence.
- Stable interview-review outputs that belong in `F:\PersonalVault\ByteDance\InterviewOS\06_复习库`.

## Current Code Workspace

Use `F:\PersonalVault\ByteDance\project\code\personalCode\NextThing\NextThingB1VCS` as the active coding, build, install, and Git workspace.

## Current UI Baseline

- 默认主题：蓝白现代卡片风格，应用级主色 `#0A84FF`，Material 基础蓝 `#4FC3F7`。
- 背景：白色、`#F7F8FC` 与浅蓝灰分层。
- 动态主题：支持浅色、深色、跟随系统和跟随天气；天气模式允许用户覆盖各天气主色。
- 紫色用途：AI、个人中心、信息 Toast、统计指标、雷雨主题和部分滑动操作等局部语义强调，不是全局品牌主色。
