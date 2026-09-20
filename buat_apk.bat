@echo off
echo ====================================================
echo MEMBUAT FILE APK BENGKEL QU
echo ====================================================
echo.
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
