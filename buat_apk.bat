@echo off
echo ====================================================
echo MEMBUAT FILE APK BENGKEL QU
echo ====================================================
echo.

if not exist "debug.keystore" (
    if exist "debug.keystore.base64" (
        echo Menyiapkan sertifikat kunci (keystore)...
        certutil -decode debug.keystore.base64 debug.keystore >nul 2>&1
    )
)

if not exist ".env" (
    if exist ".env.example" (
        copy .env.example .env >nul 2>&1
    ) else (
        type nul > .env
    )
)

echo Pastikan komputer Anda sudah terpasang Java (JDK 17).
echo Sedang memproses pembuatan APK...
echo.

call gradlew.bat assembleDebug

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo.
    echo ====================================================
    echo SUKSES! File APK berhasil dibuat!
    echo Lokasi file: app\build\outputs\apk\debug\app-debug.apk
    echo ====================================================
    echo Membuka folder tempat APK berada...
    explorer app\build\outputs\apk\debug
) else (
    echo.
    echo ====================================================
    echo Gagal membuat APK. Pastikan JDK 17 sudah terinstal.
    echo ====================================================
)
pause
