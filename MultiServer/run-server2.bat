@echo off
title MultiServer - SERVER 2 (Port 1098)
cd /d "%~dp0"
echo Dang khoi dong Server 2 tren Port 1098 (RMIDATABASE)...
java -cp "server/target/server-1.0-SNAPSHOT.jar;server/target/classes;common/target/common-1.0-SNAPSHOT.jar;%USERPROFILE%/.m2/repository/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar;%USERPROFILE%/.m2/repository/com/formdev/flatlaf/3.5.4/flatlaf-3.5.4.jar" com.multiserver.server.ServerMain 2
pause
