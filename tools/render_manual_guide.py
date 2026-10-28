"""Render the project manual to a standalone readable HTML file."""
from pathlib import Path
import markdown

root=Path(__file__).resolve().parents[1]
source=root/'docs/MANUAL-TESTING-GUIDE.md'
renderer=markdown.Markdown(extensions=['tables','fenced_code','toc'])
body=renderer.convert(source.read_text(encoding='utf-8'))
style='''
*{box-sizing:border-box}body{margin:0;background:#f4f7f5;color:#172d28;font:16px/1.7 system-ui,sans-serif}
main{max-width:1060px;margin:36px auto;background:white;padding:42px;border-radius:12px}
h1{font-size:32px;line-height:1.25}h2{margin-top:48px;line-height:1.35;padding-top:12px;border-top:2px solid #eaf3ed;scroll-margin-top:20px}
a{color:#126748}p,li{max-width:90ch}li{margin:8px 0}table{width:100%;border-collapse:collapse;margin:22px 0;font-size:14px}
th,td{text-align:left;vertical-align:top;padding:10px;border:1px solid #cfddd4}th{background:#eaf3ed}
pre{overflow:auto;background:#143e35;color:white;padding:18px;border-radius:8px;font-size:14px}code{overflow-wrap:anywhere}
nav{background:#eaf3ed;padding:20px;border-radius:8px}nav ul{padding-left:22px}nav>ul>li>ul{columns:2;column-gap:24px}
.intro{color:#5b7068}@media(max-width:700px){main{margin:0;padding:22px;border-radius:0}nav>ul>li>ul{columns:1}table{display:block;overflow:auto}}
@media print{body{background:white}main{margin:0;padding:0}nav{display:none}h2{break-after:avoid}pre{white-space:pre-wrap;color:black;background:#eee}tr{break-inside:avoid}}
'''
html='<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Balut Companion — Manual Testing Guide</title><style>'+style+'</style><main><p class="intro">Balut Companion 1.0.0 · Student manual testing</p><nav aria-label="Guide contents">'+renderer.toc+'</nav>'+body+'</main></html>'
destination=source.with_suffix('.html');destination.write_text(html,encoding='utf-8')
assert html.count('<h2')>=25
assert 'http://10.0.2.2:8789' in html and 'MANUAL-TEST' in html
print('Rendered guide:',destination)
print('Sections:',html.count('<h2'))
