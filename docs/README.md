# Documentación académica

## Entrega 3

Ubicación: `docs/entrega-3/`.

Estado: documentación histórica conservada como evidencia. Sus fuentes y el
PDF entregado permanecen juntos para preservar la trazabilidad académica.

## Entrega 4

Ubicación oficial: `docs/entrega-4/`.

**`docs/entrega-4/` es la documentación oficial y vigente.**

Documento fuente oficial: `docs/entrega-4/main.tex`.

PDF final: `docs/entrega-4/Informe_E4_SCLI_FUVV.pdf`.

Las fuentes asociadas están en `docs/entrega-4/secciones/`, la bibliografía en
`docs/entrega-4/referencias.bib` y las figuras externas propias de la entrega
deben almacenarse en `docs/entrega-4/figuras/`.

Para reproducir el PDF desde la raíz del repositorio:

```bash
cd docs/entrega-4
pdflatex -interaction=nonstopmode -halt-on-error main.tex
bibtex main
pdflatex -interaction=nonstopmode -halt-on-error main.tex
pdflatex -interaction=nonstopmode -halt-on-error main.tex
```

### Evidencias visuales pendientes

La auditoría del repositorio no encontró capturas reales del dashboard de
Grafana ni del flujo QR ejecutado en un dispositivo. No deben sustituirse con
imágenes sintéticas ni con capturas históricas. Cuando se obtengan de un entorno
real, deben eliminarse secretos y datos personales, registrar el SHA, la fecha,
la zona horaria y el entorno, y guardarse respectivamente como
`docs/entrega-4/figuras/panel-monitoreo.png` y
`docs/entrega-4/figuras/qr-scan.png`.

Los ADR, diagramas, evidencias e informes ISO situados en los demás
subdirectorios de `docs/` son evidencia técnica compartida. Conservan su
ubicación original y no reemplazan el documento oficial de Entrega 4.
