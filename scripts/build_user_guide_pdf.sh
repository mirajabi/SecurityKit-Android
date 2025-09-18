#!/bin/bash
# Build SecurityModule User Guide PDF
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
DOCS_DIR="$ROOT_DIR/docs"
INPUT_MD="$DOCS_DIR/SecurityModule-User-Guide.md"
OUTPUT_PDF="$DOCS_DIR/SecurityModule-User-Guide.pdf"

if ! command -v pandoc >/dev/null 2>&1; then
  echo "❌ pandoc not found. Install with: brew install pandoc" >&2
  exit 1
fi

# Optional: use wkhtmltopdf or basic pandoc pdf engine if available
PDF_ENGINE="pdflatex"
if command -v xelatex >/dev/null 2>&1; then
  PDF_ENGINE="xelatex"
fi

echo "📄 Building PDF: $OUTPUT_PDF"
pandoc \
  -V geometry:margin=1in \
  -V mainfont="Helvetica" \
  --pdf-engine="$PDF_ENGINE" \
  -o "$OUTPUT_PDF" \
  "$INPUT_MD"

echo "✅ PDF generated at: $OUTPUT_PDF"
