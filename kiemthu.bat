@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul
cd /d "%~dp0"

if not exist "docs" mkdir "docs"
set "REPORT=docs\ket-qua-kiem-thu.txt"

echo ====================================================================== > "%REPORT%"
echo   KET QUA KIEM THU TU DONG - DE TAI 30: TRO CHOI DOAN SO NHIEU NGUOI >> "%REPORT%"
echo ====================================================================== >> "%REPORT%"
echo. >> "%REPORT%"

type "%REPORT%"

echo [Buoc 1] Xoa thu muc bin/ cu va bien dich lai toan bo src/ va test/...
echo [Buoc 1] Xoa thu muc bin/ cu va bien dich lai toan bo src/ va test/... >> "%REPORT%"

if exist "bin" (
    rmdir /s /q "bin" 2>nul
)
if not exist "bin" mkdir "bin"

powershell -NoProfile -Command "$files = (Get-ChildItem -Path src,test -Recurse -Filter *.java).FullName; & javac -encoding UTF-8 -d bin -cp 'lib/junit-platform-console-standalone.jar;src;test' $files 2>&1 | Tee-Object -FilePath '%REPORT%' -Append; exit $LASTEXITCODE"
if errorlevel 1 (
    echo.
    echo ======================================================================
    echo TONG KET: HONG ^(Loi bien dich ma nguon^)
    echo ======================================================================
    echo. >> "%REPORT%"
    echo TONG KET: HONG ^(Loi bien dich ma nguon^) >> "%REPORT%"
    exit /b 1
)

echo Bien dich thanh cong!
echo Bien dich thanh cong! >> "%REPORT%"
echo. >> "%REPORT%"

set "SO_LAN_CHAY=5"
set "CO_LOI=0"

for /L %%i in (1,1,%SO_LAN_CHAY%) do (
    echo ----------------------------------------------------------------------
    echo [Buoc 2 - Lan chay %%i/%SO_LAN_CHAY%] Chay toan bo JUnit 5 tests trong test/...
    echo ---------------------------------------------------------------------- >> "%REPORT%"
    echo [Buoc 2 - Lan chay %%i/%SO_LAN_CHAY%] Chay toan bo JUnit 5 tests trong test/... >> "%REPORT%"

    powershell -NoProfile -Command "& java -jar lib/junit-platform-console-standalone.jar execute --class-path bin --scan-class-path --disable-ansi-colors 2>&1 | Tee-Object -FilePath '%REPORT%' -Append; exit $LASTEXITCODE"
    if errorlevel 1 (
        set "CO_LOI=1"
        echo [Ket qua lan %%i/%SO_LAN_CHAY%]: HONG
        echo [Ket qua lan %%i/%SO_LAN_CHAY%]: HONG >> "%REPORT%"
    ) else (
        echo [Ket qua lan %%i/%SO_LAN_CHAY%]: DAT
        echo [Ket qua lan %%i/%SO_LAN_CHAY%]: DAT >> "%REPORT%"
    )
    echo. >> "%REPORT%"
)

echo ======================================================================
echo ====================================================================== >> "%REPORT%"
if "!CO_LOI!"=="0" (
    echo TONG KET: DAT ^(Ca %SO_LAN_CHAY%/%SO_LAN_CHAY% lan chay kiem thu da luong deu thanh cong 100%%^)
    echo TONG KET: DAT ^(Ca %SO_LAN_CHAY%/%SO_LAN_CHAY% lan chay kiem thu da luong deu thanh cong 100%%^) >> "%REPORT%"
    echo Da ghi toan bo ket qua vao: %REPORT%
    exit /b 0
) else (
    echo TONG KET: HONG ^(Co phep thu that bai^)
    echo TONG KET: HONG ^(Co phep thu that bai^) >> "%REPORT%"
    echo Da ghi toan bo ket qua vao: %REPORT%
    exit /b 1
)
