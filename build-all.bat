@echo off
rem Builds both executable JARs. Requires JDK 21+ and Maven 3.9+.
cd /d "%~dp0"
for %%P in (TCPServer TCPClient) do (
  echo === Building %%P ===
  pushd %%P
  call mvn -q clean package || exit /b 1
  popd
)
echo Done.
