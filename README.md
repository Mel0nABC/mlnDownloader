<!-- SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT -->

# MlnDownloader - Multi Thread Downloader (Java)

<img width="1920" height="1080" alt="Portada" src="https://github.com/user-attachments/assets/bf485540-73c2-4735-8d3a-bb16d283bc2e" />

Proyecto en Java **gestor de descargas multihilo**, utilizando HTTP Range, procesamiento concurrente y verificación de integridad de archivos.

---

## 📌 Descripción

Este proyecto implementa un descargador de archivos que divide el contenido en múltiples partes y las descarga en paralelo usando hilos.

Incluye:

- Descarga por **HTTP Range**
- Descarga multihilo (`Thread`)
- Unión final de partes en un único archivo
- Verificación de tamaño final
- Resumen de ficheros descargados

---

## 🚀 Funcionamiento

1. El usuario introduce una URL.
2. Se realiza una petición `HEAD` para obtener el tamaño del archivo.
3. El archivo se divide en múltiples partes (chunks).
4. Cada parte se descarga en paralelo usando threads.
5. Cada chunk se guarda temporalmente como:

```
archivo_PART_0
archivo_PART_1
archivo_PART_2
...
```

6. Al finalizar:
   - Se ordenan las partes
   - Se unen en un solo archivo final
   - Se eliminan los archivos temporales

---

## ⚙️ Tecnologías usadas

- Java 21+ (`HttpClient`)
- Threads nativos (`Thread`)
- Java NIO (`Files`, `Path`)
- Spring Boot
- Spring Web
- Spring Web Socket
- Lombok

---

## ⚙️ Requisitos

- Java JDK 21 o superior
- Apache Maven 3.9.15 o superior
- Spring Boot 3.x
- Git
- Sistema operativo Linux, Windows o macOS
- Conexión a internet para descargar dependencias Maven

## ▶️ Ejecución

Compilar:

```bash
mvn clean install
```


Ejecutar:

```bash
mvn spring-boot:run
```

---

## 📊 Información mostrada

- Tamaño total del archivo
- Tamaño de cada chunk
- Número de partes
- Estado de descarga
- Tiempo total de descarga

---

## ⚠️ Limitaciones

- No incluye reintentos automáticos
- Depende de soporte HTTP Range del servidor
- No optimizado para producción
