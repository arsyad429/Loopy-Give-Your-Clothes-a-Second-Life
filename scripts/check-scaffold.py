#!/usr/bin/env python3
"""Read-only structural check, bukan pengujian acceptance produk."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
skip={'.git','.gradle','.kotlin','.idea','build','artifacts','__pycache__'}
errors=[]
screens=json.loads((root/'docs/screen-map.json').read_text())
assert len(screens)==17
assert len([s for s in screens if s['scope']=='MVP'])==13
for s in screens:
 for suffix in ['Screen','ViewModel','UiState']:
  p=root/'android/app/src/main/java/id/ac/loopy/feature'/s['feature']/(s['name']+suffix+'.kt')
  if not p.is_file(): errors.append('Missing '+str(p.relative_to(root)))
for folder in [root]+[p for p in root.rglob('*') if p.is_dir() and not any(v in skip for v in p.relative_to(root).parts)]:
 if not (folder/'README.md').is_file(): errors.append('Missing README '+str(folder.relative_to(root)))
for path in ['gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar', 'gradle/wrapper/gradle-wrapper.properties']:
 if not (root/'android'/path).is_file(): errors.append('Missing Android wrapper '+path)
for path in ['go.mod', 'go.sum', 'main.go']:
 if not (root/'backend/go'/path).is_file(): errors.append('Missing Go backend file '+path)
for i in range(1,21):
 if f'AC-{i:02}' not in (root/'docs/acceptance-tests.md').read_text(): errors.append(f'Missing AC-{i:02}')
assert len(json.loads((root/'docs/reference/papb-catalog.json').read_text()))==10
if errors: raise SystemExit('\n'.join(errors))
print('OK: 13 MVP+4 design screens, 10 PAPB sources, 20 AC, wrapper Android, backend Go, dan README setiap folder source.')
