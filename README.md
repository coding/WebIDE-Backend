# Coding WebIDE Backend
[![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://github.com/Coding/WebIDE/blob/master/LICENSE) [![Build Status](https://travis-ci.org/Coding/WebIDE-Backend.svg?branch=master)](https://travis-ci.org/Coding/WebIDE-Backend) [![Docker Stars](https://img.shields.io/docker/stars/webide/backend.svg)](https://hub.docker.com/r/webide/backend 'DockerHub') [![Docker Pulls](https://img.shields.io/docker/pulls/webide/backend.svg)](https://hub.docker.com/r/webide/backend 'DockerHub')

本项目是开源版 [WebIDE](https://ide.coding.net) 的后端程序。

## 开发环境

本项目使用 Maven 作为构建系统, 开发 IDE 可以为 Intellj IDEA 或 Eclipse, 只需要在按照 maven 类型导入即可。

## 配置文件

* `/src/main/resources/application.properties` 包括用户、项目、数据库等配置

> **SPACE_HOME:** 存放 workspace 的目录，默认为 ${"user.home"}/.workspace  
> **server.port:** 应用启动的端口

## 包说明

> **config:** 项目配置类  
> **dto:** dto 类  
> **entity:** 实体类  
> **event:** 与事件相关的类  
> **git:** 扩展 jgit 的类  
> **model:** 一些数据结构  
> **repository:** repository 类, 用于数据库查询  
> **service:** 服务类
> **tty:** terminal 的 java 实现  
> **utils:** 工具类  
> **web:** 与 web 相关的类, 比如 controller 等  

## 项目运行

本项目将 [WebIDE-Frontend](https://github.com/Coding/WebIDE-Frontend) 前端打包成 `webjar`, 作为 maven 依赖引入, 因此在运行本项目前需要先打包前端项目。

可以参见 [WebIDE](https://github.com/Coding/WebIDE) 项目的[脚本](https://github.com/Coding/WebIDE/blob/master/ide.sh), 该脚本提供了一键编译、运行 `WebIDE` 的功能)。

如果想手动打包前端, 可以参见 `WebIDE-Frontend-Webjars` 项目。

打包完成后, 使用 `mvn spring-boot:run` 启动项目即可。
