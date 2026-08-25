"""Strip the standalone-HTML wrapper from testing/regression-checklist.html so the
result can be published via the Artifact tool (which auto-wraps <!doctype>/<html>/<head>/<body>
itself and expects the file to start with a bare <title>).

Usage: python extract_artifact_fragment.py <repo_html_path> <fragment_out_path>
"""

import re
import sys


def main() -> None:
    if len(sys.argv) != 3:
        print("usage: extract_artifact_fragment.py <repo_html_path> <fragment_out_path>")
        raise SystemExit(1)

    src_path, dst_path = sys.argv[1], sys.argv[2]
    with open(src_path, encoding="utf-8") as f:
        text = f.read()

    text = text.replace('<!doctype html>\n', '')
    text = text.replace('<html lang="en">\n', '')
    text = text.replace('<head>\n', '')
    text = text.replace('<meta charset="utf-8">\n', '')
    text = text.replace('<meta name="viewport" content="width=device-width, initial-scale=1">\n', '')
    text = re.sub(r'<meta name="description"[^>]*>\n?', '', text)
    text = text.replace('</head>\n<body>\n', '')
    text = re.sub(r'\n?</body>\s*</html>\s*$', '\n', text)

    with open(dst_path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)

    print("wrote", dst_path, "(", len(text), "chars )")


if __name__ == "__main__":
    main()
