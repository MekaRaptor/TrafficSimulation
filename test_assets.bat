@echo off
echo ===== Asset-Enhanced Traffic Simulation Test =====
echo.

echo Asset'lerin durumu kontrol ediliyor...
echo.

REM Create asset directories if they don't exist
if not exist "assets" (
    echo Asset klasörleri oluşturuluyor...
    mkdir assets
    cd assets
    mkdir vehicles roads lights environment
    cd ..
    echo Asset klasörleri oluşturuldu!
    echo.
)

echo Asset klasörleri hazır.
echo.

echo Enhanced simülasyon başlatılıyor...
echo Not: Asset'ler bulunamazsa otomatik olarak geliştirilmiş şekil çizimi kullanılacak.
echo.

REM Go back to parent directory and run the enhanced simulation
cd ..
call run_enhanced_simulation.bat 