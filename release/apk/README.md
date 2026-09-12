# APK release firmado

Los APK release firmados no se almacenan directamente en Git. Se generan,
firman, verifican y publican mediante GitHub Actions para evitar versionar
binarios y material privado de firma.

El artifact oficial correspondiente al HEAD auditado
`07d845849dc51ff215fb82c83663e316c783a85e` es:

```text
scli-mobile-release-07d845849dc51ff215fb82c83663e316c783a85e
```

Fue publicado por el run de CI `34687754162`. El job
`Build - Android release signed` terminó con estado `completed/success` después
de ejecutar `zipalign`, firmar con `apksigner`, verificar la firma mediante
`apksigner verify`, generar `SHA256SUMS.txt` y comprobar el checksum.

El artifact contiene exclusivamente:

```text
scli-mobile-0.1.0-release.apk
SHA256SUMS.txt
```

Para descargarlo, abra el repositorio en GitHub y siga esta ruta:

```text
GitHub → Actions → CI → run 34687754162 → Artifacts
```

El keystore y sus credenciales permanecen fuera del repositorio y se gestionan
mediante GitHub Secrets durante la ejecución del workflow.
