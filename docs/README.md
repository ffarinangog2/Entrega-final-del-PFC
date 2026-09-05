# Documentación académica del SCLI

## Documento oficial de Entrega 4

El entregable académico vigente es [`entrega-4/main.tex`](entrega-4/main.tex).
Sus fuentes asociadas se encuentran exclusivamente en:

- `docs/entrega-4/secciones/`;
- `docs/entrega-4/referencias.bib`;
- `docs/entrega-4/figuras/`.

Para reproducir el PDF, desde la raíz del repositorio:

```bash
cd docs/entrega-4
pdflatex main.tex
bibtex main
pdflatex main.tex
pdflatex main.tex
```

Se necesita una distribución LaTeX con `pdflatex`, BibTeX y los paquetes
declarados en `main.tex` (`babel` con español, `geometry` e `hyperref`). El
archivo resultante es `docs/entrega-4/main.pdf`. La secuencia de cuatro pasos
genera los auxiliares, procesa `referencias.bib` y resuelve las citas y
referencias cruzadas.

## Histórico de Entrega 3

Los siguientes artefactos se conservan únicamente como antecedente y evidencia
histórica de Entrega 3; **no constituyen el documento oficial de Entrega 4**:

- `docs/main.tex`;
- `docs/Referencias.bib`;
- `docs/secciones/`;
- `docs/Informe_E3_SCLI_LATEX.pdf`.

Estos archivos no se eliminan ni se mezclan con E4 porque forman parte de la
trazabilidad académica del proyecto. Los ADR, diagramas, evidencias e informes
ISO ubicados en otros subdirectorios de `docs/` son material técnico de apoyo y
no reemplazan el documento oficial señalado arriba.
