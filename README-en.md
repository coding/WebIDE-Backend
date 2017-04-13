# Coding WebIDE Backend
[![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://github.com/Coding/WebIDE/blob/master/LICENSE) [![Build Status](https://travis-ci.org/Coding/WebIDE-Backend.svg?branch=master)](https://travis-ci.org/Coding/WebIDE-Backend) [![Docker Stars](https://img.shields.io/docker/stars/webide/backend.svg)](https://hub.docker.com/r/webide/backend 'DockerHub') [![Docker Pulls](https://img.shields.io/docker/pulls/webide/backend.svg)](https://hub.docker.com/r/webide/backend 'DockerHub')

This repo is the backend of **Coding [WebIDE](https://ide.coding.net) Community Edition**

## Dev Environment

The backend is written in Java, and uses Maven as build tool. We recommend IntelliJ IDEA or Eclipse as IDE.

## Configuration

Everything in `/src/main/resources/application.properties`, including user info, project and database configs, etc.

## Packages

> **config:** Config classes  
> **dto:** dto classes  
> **entity:** Entity classes  
> **event:** Event-related classes  
> **git:** Classes that extend jgit class  
> **model:** Data model/structure  
> **repository:** Repository class, for database query purpose  
> **service:** Service classes  
> **tty:** A Java implementation of terminal  
> **utils:** Utils classes  
> **web:** Web-related classes, controllers etc.  

## Run the project

The backend first packs the frontend to webjar, and import as a maven dependency, thus packing the frontend is required before run.

Please refer to the script in `WebIDE-Workspace` repo, which provides a command to compile and run the whole project.

If you want to pack the frontend manually, please refer to `WebIDE-Frontend-Webjars` repo.

Once done packing, start the poject with `mvn spring-boot:run`.
