@echo off
setlocal
set "SCREEPS_PYTHON=%~dp0.venv\Scripts\python.exe"
if not exist "%SCREEPS_PYTHON%" (
    echo Create the environment: python -m venv .venv
    echo Install dependencies: .venv\Scripts\python.exe -m pip install -r tools\requirements.txt
    exit /b 1
)
"%SCREEPS_PYTHON%" -X utf8 "%~dp0tools\screeps_cli.py" %*
exit /b %errorlevel%
