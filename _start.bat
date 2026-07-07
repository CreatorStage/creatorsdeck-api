@echo off
set SPRING_PROFILES_ACTIVE=dev
call mvnw.cmd spring-boot:run
