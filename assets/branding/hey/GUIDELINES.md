# Hey by Ars — panduan identitas final

Versi 1.0 · 11 Oktober 2026 · unit master: 1000 × 1000.

## 1. Keputusan desain

**Satu rounded square hitam dengan satu kata putih, Hey.** Ini adalah rekomendasi final dalam arah yang telah dipilih, bukan klaim bahwa estetika dapat dinilai secara universal. Logo terasa personal melalui sapaan pendeknya, kuat melalui massa huruf, dan tenang melalui bentuk serta warna yang terbatas.

Referensi utama yang diperiksa: gambar “Ikon Aplikasi Hitam Hey.” dan screenshot 1000187299.jpg. Pada gambar tersebut, kata mengisi sekitar tujuh persepuluh lebar kotak; H besar, e padat, y berekor, titik bulat. Bayangan dan latar presentasi di referensi tidak dimasukkan ke aset logo.

### Bentuk

Radius sudut **0,24 × sisi** dipilih untuk memberi lengkungan yang terasa modern dengan sisi lurus yang masih kokoh. Bentuk ini rounded square dengan quarter-circle radius, bukan squircle Apple dan bukan capsule. Pada bidang 1000, radius 240; bagian lurus sisi sepanjang 520 unit. Tidak ada bevel, stroke, shadow, atau gradient.

### Lettering

Huruf adalah geometri khusus dengan sifat bold sans serif: H tegak dan terbuka, e berkontur membulat dengan counter yang cukup besar, y memiliki diagonal dan ekor melengkung, serta titik berbentuk lingkaran. Logo tidak ditetapkan dengan mengetik nama font biasa, sehingga hasil konsisten di Android, SVG, dan gambar.

H memakai stem 60 unit pada geometri huruf dan palang 58 unit; e diberi counter yang jelas serta terminal terbuka; y menjaga ekor sebagai karakter khas. Titik berdiameter sama dengan stem H pada geometri awal. Ritme ini mengikat titik dengan kata, tanpa membuatnya menjadi tanda baca yang terlalu kecil atau menjadi ornamen baru.

Lebar lettering utama **720/1000 = 72%** menjaga dominasi sesuai referensi. Lebih besar dari ini akan mengurangi napas di kanan titik; lebih kecil akan melemahkan arahan pengguna tentang tulisan yang dominan. Angka ini merupakan keputusan desain untuk komposisi final.

### Optical balance

Bounding box lettering berada 10 unit ke kanan dari pusat geometris bidang. Massa H di kiri dan ruang terbuka titik di kanan membuat penyesuaian ini layak. Bounding box secara vertikal berada sedikit di bawah pusat: ekor y membuat pusat kotak huruf berbeda dari pusat visual badan kata. Karena itu, jangan memakai otomatisasi “align center” editor untuk menggantikan transformasi master.

Posisi final dipilih lewat pemeriksaan render. Koordinat bounding box adalah ukuran geometri yang pasti; “optical center” adalah penilaian desain, bukan klaim bahwa centroid pixel harus tepat (500,500).

## 2. Spesifikasi utama

| Elemen | Nilai final pada bidang 1000 |
| --- | --- |
| Background | #000000, opaque |
| Lettering | #FFFFFF, opaque |
| Rasio bidang | 1:1 |
| Radius | 240 = 24% sisi |
| Lettering width | 720 = 72% sisi |
| Batas kiri / kanan | x = 150 / 870 |
| Batas atas / bawah | y = 346 / 706,976 |
| Padding kiri / kanan | 150 / 130 |
| Padding atas / bawah | 346 / 293,024 |
| Pusat bounding box lettering | (510; 526,488) |
| Baseline H | y = 619,171 |
| Tinggi H | 273,171 |
| Stem H | 58,537 |
| Diameter titik | 58,537 = 21,43% tinggi H |
| Pusat titik | (840,732; 589,902) |
| Bawah titik | y = 619,171, sejajar baseline H |
| Tinggi total dengan ekor y | 360,976 |

**Safe area internal master:** gunakan keseluruhan posisi lettering yang telah dikunci. Jangan mengubah padding secara mandiri. Safe area ini berbeda dari zona aman adaptive icon Android.

**Clear space eksternal:** minimal 0,125 × sisi ikon pada keempat sisi. Contoh: ikon 80 px memerlukan ruang luar 10 px. Grid launcher ditentukan OS; aturan clear space ini untuk komposisi brand, slide, web, dan materi presentasi.

## 3. Varian resmi

| Varian | File | Pemakaian |
| --- | --- | --- |
| Primary | hey-primary.svg | Brand utama, avatar, preview aplikasi, komposisi terang |
| App icon | hey-adaptive-foreground.svg + background; XML launcher | Android dengan mask OS; background full bleed |
| Monochrome | hey-monochrome.svg | Lettering hitam transparan untuk reproduksi satu tinta; koordinat tetap pada bidang master |
| Monochrome Android | ic_launcher_monochrome.xml | Siluet huruf dan titik transparan di luar glyph; tint dipilih OS |
| Inverted | hey-inverted.svg | Rounded square putih, lettering hitam untuk bidang gelap |
| Small-size | hey-small.svg | Ukuran 16–32 px; titik diperbesar tanpa mengganti huruf |
| White wordmark | hey-wordmark-white.svg | Lettering putih transparan pada bidang hitam; varian pendukung |

Primary sendiri sudah hitam-putih. “Monochrome” transparan tidak menggantikan primary sebagai avatar: gunakan kotak primary untuk avatar. Background putih inverse tetap opaque di dalam kotak. Di bidang hitam penuh, gunakan inverse agar kontur ikon terbaca, atau white wordmark untuk penempatan teks brand. Jangan menambahkan border baru pada primary.

### Small-size refinement

H/e/y, posisi, serta radius identik. Radius titik di geometri huruf bertambah dari 30 ke 34: diameter pada master menjadi 66,341 (+13,33%). Pusat titik tetap, sehingga tepi kanan master small menjadi 873,902. Baseline titik lebih rendah 3,902 unit daripada primary; ini merupakan koreksi optik terbatas pada ukuran kecil.

Ukuran yang direkomendasikan: **32 px atau lebih** untuk identitas utama; **24 px** untuk pemakaian kecil yang perlu dibaca; **16 px** hanya fallback favicon. Pada 16 px, e dan y memang terbatas oleh sampling pixel; jangan menjanjikan keterbacaan setara ukuran besar. Android launcher gunakan asset adaptive resmi; jangan menggantinya dengan favicon.

## 4. Android: anatomi terpisah

Layer adaptive memiliki kanvas **108 × 108 dp**. Background hitam memenuhi seluruh kanvas, foreground hanya glyph putih. OS menentukan mask. Tidak ada rounded square kedua di foreground. Sumber resmi Android mengatur ukuran layer, pemisahan foreground/background, dan layer monochrome untuk themed icon [1].

| Elemen adaptive | Nilai |
| --- | --- |
| Kanvas | 108 × 108 dp |
| Acuan viewport mask | 72 × 72 dp, inset 18 dp |
| Zona aman konservatif paket | Lingkaran diameter 66 dp, pusat (54,54) |
| Lettering width | 59 dp |
| Posisi kiri / atas lettering | (25,41) dp |
| Batas kanan / bawah | (84; 70,580) dp |
| Dominasi lebar viewport | 59/72 = 81,94% |
| Diameter titik | 4,797 dp pada kanvas 108 |

Seluruh tinta foreground diuji terhadap lingkaran aman, bukan hanya kotak batas 66 × 66. Ini menjaga titik dan sudut H saat mask lingkaran diterapkan. Proporsi launcher terlihat lebih besar daripada brand master; karakter huruf dan titik tetap sama. Penyesuaian ini khusus bidang OS, bukan arah logo baru.

Radius 24% berlaku untuk ikon brand dan PNG legacy rounded-square. Mask launcher Android, Google Play, dan bentuk themed icon dikendalikan sistem; bentuk rounded square tidak dapat dipaksakan di semua perangkat. Identitas yang dipertahankan di semua mask adalah Hey. dan warna normalnya.

Themed icon menggunakan siluet huruf dan titik saja, dengan latar transparan. Warna akan mengikuti tema pengguna; hitam-putih persis tidak dijamin dalam mode ini. Monochrome bukan gambar kotak hitam opaque yang kemudian ditint OS [1].

## 5. Splash

Pilihan: lettering putih pada seluruh window hitam. Drawable vektor 288 × 288 dp, lettering selebar 170 dp, offset (61,107), seluruh tinta di dalam lingkaran diameter 192 dp. Tidak memakai icon background tambahan. Ini mengikuti jalur splash tanpa background ikon dalam spesifikasi resmi [2].

Splash bukan layar onboarding. Hindari delay buatan dan hindari dua splash berturut-turut. Jika aplikasi memakai tema malam berbeda, pastikan starting theme tetap hitam-putih. Contoh integrasi ada di folder android/examples; sesuaikan tema aplikasi yang sebenarnya.

## 6. Do / don't

**Do:** gunakan master outline; pertahankan rasio 1:1 dan transformasi huruf; gunakan clear space; gunakan inverse di bidang gelap; pakai variant sesuai media; gunakan PNG besar untuk raster dan SVG untuk kebutuhan skalabel; simpan alpha di luar sudut ikon brand.

**Don't:** hilangkan titik; ketik ulang dengan font pengganti; ubah jarak antarhuruf; regangkan; tambahkan “by Ars” di dalam ikon; pakai gradient, glass, texture, bevel, glow, shadow atau simbol AI/browser; bake mask OS ke adaptive foreground; gunakan export Play Store sebagai foreground launcher; menganggap empat sisi padding huruf harus sama secara matematis.

Pada background foto/ramai, gunakan primary atau inverse lengkap dengan clear space, ditempatkan di area yang tenang. Jangan membuat kata transparan di atas detail foto. Logo tetap datar; bayangan yang dibuat launcher/store adalah perlakuan sistem di luar aset.

## 7. Export dan sumber

SVG adalah sumber utama. PNG utama tersedia 4096, 1024, 512, 256, 128, 64, 48, dan 32 px. PNG 4096 cocok untuk materi besar, tetapi launcher Android memakai vector drawable agar ringan. Warna aset hitam-putih sRGB; versi Play Store dibuat RGBA 512 × 512 dengan background full square dan tanpa sudut/shadow yang dibake [3].

Konstruksi lettering dibuat langsung dari path. Jangan mengedit SVG dan XML secara terpisah untuk perubahan huruf: ubah sumber geometrinya lalu regenerate dan periksa ulang seluruh ukuran.

Sumber resmi, diperiksa 11 Oktober 2026 WIB:

[1] https://developer.android.com/develop/ui/compose/system/icon_design_adaptive

[2] https://developer.android.com/develop/ui/views/launch/splash-screen

[3] https://developer.android.com/distribute/google-play/resources/icon-design-specifications

## 8. Verifikasi dan batas

Pemeriksaan lokal mencakup render actual SVG, preview tiga mask, XML well-formed, path tanpa font dependency, safe-circle adaptive/splash, dimensi PNG, dan keberadaan titik pada fallback kecil. Hasil numerik dicatat dalam QA_REPORT.json. Pengujian pada HP Hey, launcher OEM, gerakan parallax nyata, kompilasi resource dengan Android SDK, serta build Gradle belum dilakukan di paket ini. Kesiapan aset tidak sama dengan bukti integrasi APK.
