#!/usr/bin/env python3
"""Bootstrap exact reviewed companion sources; never skip tests or use floating refs."""
import hashlib, json, pathlib, re, subprocess, urllib.request, xml.etree.ElementTree as ET
ROOT=pathlib.Path(__file__).resolve().parents[2]
pins=json.loads((ROOT/'tools/ci/companions.json').read_text())
for pin in pins.values():
    if not re.fullmatch(r'FainNeito/[A-Za-z0-9_.-]+',pin['repository']) or not re.fullmatch(r'[0-9a-f]{40}',pin['sha']):
        raise SystemExit('Invalid companion repository or immutable SHA')
def run(*args, cwd=None): subprocess.run(args,cwd=cwd,check=True)
work=ROOT/'.ci-deps'; work.mkdir(exist_ok=True)
renderer=pins['renderer']; target=work/'renderer'
if not target.exists():
    run('git','init',str(target))
    run('git','remote','add','origin','https://github.com/'+renderer['repository']+'.git',cwd=target)
run('git','fetch','--depth=1','origin',renderer['sha'],cwd=target)
run('git','checkout','--detach',renderer['sha'],cwd=target)
actual=subprocess.check_output(['git','rev-parse','HEAD'],cwd=target,text=True).strip()
if actual!=renderer['sha']: raise SystemExit('Renderer checkout differs from pin')
ns={'m':'http://maven.apache.org/POM/4.0.0'}
provided=ET.parse(target/'pilot/pom.xml').find('m:version',ns).text
required=ET.parse(ROOT/'pom.xml').find(".//m:dependency[m:artifactId='EnthusiaAdvancements-pilot']/m:version",ns).text
if provided!=renderer['version'] or provided!=required: raise SystemExit('Renderer version does not match the consumer POM')
run('mvn','-B','-ntp','-f',str(target/'pilot/pom.xml'),'clean','install')
rose=pins['rosechat']; url='https://raw.githubusercontent.com/'+rose['repository']+'/'+rose['sha']+'/'+rose['path']
request=urllib.request.Request(url,headers={'User-Agent':'Enthusia-PR-Cleanup'})
with urllib.request.urlopen(request,timeout=60) as response: content=response.read()
if hashlib.sha256(content).hexdigest()!=rose['sha256']: raise SystemExit('RoseChat API source checksum mismatch')
contract=work/'rosechat/PresenceMessageEvent.java'; contract.parent.mkdir(exist_ok=True); contract.write_bytes(content)
print('Verified companion sources: renderer '+renderer['sha']+'; RoseChat '+rose['sha'])
