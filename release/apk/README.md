# APK release firmado

Los APK release firmados se generan, firman, verifican y publican mediante
GitHub Actions. Se preserva en este directorio una copia verificada del binario
de entrega en `scli-mobile-0.1.0-release.apk` y su checksum en
`SHA256SUMS.txt`; las claves privadas y contraseñas de firma no se almacenan en
Git.

El artifact de origen correspondiente al SHA fuente
`0b755310a0acf34da2456290a4f978475a8e17f9` es:

```text
scli-mobile-release-0b755310a0acf34da2456290a4f978475a8e17f9
```

Fue publicado por el run de CI `34688947156`. El job
`Build - Android release signed` terminó con estado `completed/success` después
de ejecutar `zipalign`, firmar con `apksigner`, verificar la firma mediante
`apksigner verify`, generar `SHA256SUMS.txt` y comprobar el checksum incluido.

El artifact contiene exclusivamente:

```text
scli-mobile-0.1.0-release.apk
SHA256SUMS.txt
```

Para descargarlo, abra el repositorio en GitHub y siga esta ruta:

```text
GitHub → Actions → CI → run 34688947156 → Artifacts
```

Actions continúa siendo la fuente reproducible del proceso de generación,
firma y verificación. El keystore y sus credenciales permanecen fuera del
repositorio y se gestionan mediante GitHub Secrets durante la ejecución del
workflow.
