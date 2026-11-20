# Evaluar coche — app Android con botón flotante

Muestra un botón 🕵️ flotante sobre la app del portal de coches (coches.net, etc.). Al pulsarlo lee el texto visible del anuncio
(precio, km, año, combustible, matrícula, marca/modelo, vendedor…) y abre el informe de tu aplicación "Evaluar compra".

## Cómo conseguir el APK (sin instalar nada)
1. Crea un repositorio en GitHub y sube el contenido de esta carpeta.
2. Pestaña **Actions → Compilar APK → Run workflow**.
3. Al terminar, descarga el artefacto `evaluar-coche-apk` (app-debug.apk), pásalo al móvil e instálalo
   (permite "instalar apps desconocidas" para el navegador/gestor de archivos).

Alternativa: abrir la carpeta con **Android Studio** → *Build > Build APK(s)*.

## Configuración en el móvil
1. Abre la app → escribe la URL de tu servidor, p. ej. `https://tudominio.es/portal/` (el móvil debe poder llegar a ella;
   `localhost` de tu PC NO sirve; necesitas dominio/túnel con HTTPS).
2. Ajustes → Accesibilidad → Evaluar coche → Activar. (Android 13+: Ajustes → Apps → Evaluar coche → ⋮ → *Permitir ajustes restringidos*.)
3. Abre un anuncio en la app del portal: aparece el botón. Pulsa → informe.
   Arrástralo para moverlo. Desplaza la ficha para que se vea precio, km y descripción antes de pulsar.

## Notas honestas
- Este proyecto NO se ha compilado ni probado en un móvil real (aquí no hay Android SDK). Puede requerir algún retoque al compilar.
- El nombre de paquete de coches.net no está verificado: el botón aparece en apps cuyo paquete contenga "coches", "ocasion"…
  Si no sale, marca «Mostrar en cualquier app» y dime el nombre real.
- Si la app del portal oculta texto a accesibilidad, se leerá menos; el motor evalúa lo que encuentre.
- En este modo no se analizan las fotos (solo el texto visible). Para fotos, comparte el enlace con «Compartir → Evaluar coche».
- Solo se instala por fuera de Google Play (sideload). Nada se envía a terceros: el texto va a tu servidor.
