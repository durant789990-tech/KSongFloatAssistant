@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

set "REPO=%CD%"
set "GIT=git -c safe.directory=%REPO%"
set "GIT_USER=git -c safe.directory=%REPO% -c user.name=durant789990-tech -c user.email=durant789990-tech@users.noreply.github.com"

echo [1/4] Gradle assembleDebug ...
call gradlew.bat assembleDebug
if errorlevel 1 (
  echo Build failed.
  exit /b 1
)

echo [2/4] Copy APK to release_apk\KSongAssistant.apk ...
if not exist "release_apk" mkdir "release_apk"
if not exist "app\build\outputs\apk\debug\app-debug.apk" (
  echo APK not found: app\build\outputs\apk\debug\app-debug.apk
  exit /b 1
)
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "release_apk\KSongAssistant.apk" >nul

echo [3/4] git add and commit ...
%GIT% add .
%GIT_USER% commit -m "Fix AI key execution pass and update launcher icon"
if errorlevel 1 (
  echo Nothing to commit or commit failed.
)

for /f "usebackq delims=" %%b in (`git -c "safe.directory=%REPO%" rev-parse --abbrev-ref HEAD`) do set "BRANCH=%%b"

echo [4/4] git push origin %BRANCH% ...
%GIT% push origin %BRANCH%
if errorlevel 1 (
  echo Push failed. Check GitHub login/credentials.
  exit /b 1
)

echo Done. APK: release_apk\KSongAssistant.apk
exit /b 0
