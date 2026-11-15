@echo off
title MultiServer - CLIENT
cd /d "%~dp0"
echo Dang khoi dong Client Ngau Nhien Ket Noi Den Server...
java -cp "client/target/client-1.0-SNAPSHOT.jar;client/target/classes;common/target/common-1.0-SNAPSHOT.jar;%USERPROFILE%/.m2/repository/com/formdev/flatlaf/3.5.4/flatlaf-3.5.4.jar" com.multiserver.client.ClientMain
pause

