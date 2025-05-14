# Trafik Simülasyonu Projesi

Bu proje, şehir trafiğini simüle eden bir Java uygulamasıdır. Proje, JavaFX kullanarak grafiksel bir arayüz sunar ve çeşitli trafik senaryolarını simüle etmek için çoklu iş parçacıkları (thread) kullanır.

## Özellikler

- Ayarlanabilir şehir ızgara boyutu
- Ayarlanabilir araç sayısı
- Trafik ışıkları ve kavşak simülasyonu
- Trafik sıkışıklığı görselleştirmesi
- Simülasyon hızı kontrolü

## Gereksinimler

- Java 17 veya üzeri
- Maven 3.6 veya üzeri
- JavaFX 17

## Kurulum ve Çalıştırma

1. Projeyi klonlayın:
```
git clone https://github.com/kullanici/traffic-simulation.git
cd traffic-simulation
```

2. Maven ile projeyi derleyin:
```
mvn clean compile
```

3. Uygulamayı çalıştırın:
```
mvn javafx:run
```

Veya alternatif olarak:
```
mvn exec:java
```

## Proje Yapısı

- `com.trafficsim.core.Main`: Uygulamanın giriş noktası
- `com.trafficsim.core.TrafficSimulationGUI`: Kullanıcı arayüzü ve simülasyon görselleştirmesi
- `com.trafficsim.core.CityMap`: Şehir haritası ve simülasyon mantığı
- `com.trafficsim.core.Road`: Yol sınıfı
- `com.trafficsim.core.Intersection`: Kavşak sınıfı
- `com.trafficsim.core.TrafficLight`: Trafik ışığı sınıfı
- `com.trafficsim.core.Vehicle`: Araç sınıfı

## Kullanım

1. Uygulama başlatıldığında, sağ taraftaki kontrol panelinden simülasyon parametrelerini ayarlayabilirsiniz:
   - Grid Size: Şehir ızgara boyutunu ayarlar
   - Vehicle Count: Simülasyondaki araç sayısını ayarlar
   - Simulation Speed: Simülasyon hızını ayarlar

2. Kontrol butonları:
   - Start Simulation: Simülasyonu başlatır
   - Pause/Resume: Simülasyonu duraklatır veya devam ettirir
   - Reset: Simülasyonu sıfırlar

3. Alt kısımda simülasyon istatistiklerini görebilirsiniz:
   - Araç sayısı
   - Toplam trafik sıkışıklığı
   - Ortalama bekleme süresi

## Lisans

Bu proje [MIT Lisansı](LICENSE) altında lisanslanmıştır.