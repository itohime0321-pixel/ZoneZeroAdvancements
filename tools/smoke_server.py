"""Run a real isolated NeoForge 1.21.1 dedicated test server and verify loaded definitions.
No player world, external mod JAR, production config, or online service is modified.
The local test server is accessible only from the runner's loopback interface.
"""
import json
import os
import re
import secrets
import signal
import socket
import struct
import subprocess
import tempfile
import time
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
EXPECTED=len(json.loads((ROOT/'art/icon_manifest.json').read_text(encoding='utf-8')))
BASE=ROOT/'build/smoke-tests'
BASE.mkdir(parents=True,exist_ok=True)
RUN=Path(tempfile.mkdtemp(prefix='server-',dir=BASE))
LOG=RUN/'console.log'

def unused_port():
    with socket.socket() as sock:
        sock.bind(('127.0.0.1',0))
        return sock.getsockname()[1]

game_port=unused_port()
rcon_port=unused_port()
while rcon_port==game_port:rcon_port=unused_port()
password=secrets.token_hex(16)
(RUN/'eula.txt').write_text('eula=true\n')
(RUN/'server.properties').write_text('\n'.join([
    'server-ip=127.0.0.1',f'server-port={game_port}', 'online-mode=false',
    'enable-rcon=true',f'rcon.port={rcon_port}',f'rcon.password={password}',
    'level-name=smoke-world','level-type=minecraft:flat',
    'generate-structures=false','view-distance=2','simulation-distance=2',
    'spawn-protection=0','max-players=1','motd=ZoneZero isolated smoke test',''
]))

def read_exact(sock,size):
    result=bytearray()
    while len(result)<size:
        chunk=sock.recv(size-len(result))
        if not chunk:raise EOFError('RCON disconnected')
        result.extend(chunk)
    return bytes(result)

def packet(sock,request_id,kind,body):
    data=body.encode('utf-8')+b'\0\0'
    sock.sendall(struct.pack('<iii',len(data)+8,request_id,kind)+data)

def response(sock):
    n=struct.unpack('<i',read_exact(sock,4))[0]
    if not 10<=n<=4*1024*1024:raise ValueError('Invalid RCON packet size')
    data=read_exact(sock,n)
    req,kind=struct.unpack_from('<ii',data)
    return req,kind,data[8:-2].decode('utf-8',errors='replace')

def rcon(command,shutdown=False):
    with socket.create_connection(('127.0.0.1',rcon_port),timeout=5) as sock:
        packet(sock,100,3,password)
        auth=response(sock)
        if auth[0]!=100 or auth[1]!=2:raise RuntimeError('RCON authentication failed')
        packet(sock,101,2,command)
        try:
            req,kind,body=response(sock)
            if req!=101 or kind!=0:raise RuntimeError('Unexpected RCON command response')
            return body
        except (EOFError,ConnectionError):
            if shutdown:return ''
            raise

def text():
    return LOG.read_text(encoding='utf-8',errors='replace')

with LOG.open('wb') as out:
    proc=subprocess.Popen(['gradle','--no-daemon','--console=plain',
                           f'-PzzSmokeDir={RUN}','runSmokeServer'],
                          cwd=ROOT,stdout=out,stderr=subprocess.STDOUT,start_new_session=True)
    try:
        limit=time.monotonic()+420
        marker=f'ZoneZero: loaded {EXPECTED} advancement definitions (including root).'
        while time.monotonic()<limit:
            log=text()
            if proc.poll() is not None:raise RuntimeError('Server exited before smoke checks')
            if marker in log and re.search(r'Done \([0-9.]+s\)!',log):
                try:
                    result=rcon('list')
                    if not result:raise RuntimeError('Server list command returned no response')
                    break
                except (OSError,EOFError):
                    pass
            time.sleep(1)
        else:raise TimeoutError('Dedicated server did not load all advancement definitions in time')
        if 'ZoneZero: concrete-break hook applied.' not in text():
            raise AssertionError('Concrete block-removal mixin did not apply')
        print('PASS: concrete-break hook actually applied on NeoForge.',flush=True)
        if 'ZoneZero test: verified all 203 unique compact positions.' not in text():
            raise AssertionError('Compact layout was not actually applied to all loaded advancement definitions')
        print('PASS: actual server applied all 203 compact coordinates.',flush=True)
        # The test-only server posts two visible completed events, plus root,
        # another namespace and a repeat while announceAdvancements is false.
        # Only two chat announcements should appear, each with its flavor introduction.
        log=text()
        if 'ZoneZero test: verified white prefix, green bracketed title, white description.' not in log:
            raise AssertionError('Expected actual Minecraft chat components to have white/green/white colors')
        if 'ZoneZero test: all 202 announcements contain only the flavor introduction.' not in log:
            raise AssertionError('Expected concise announcements for all achievements')
        lines=[line for line in log.splitlines() if 'ZZ_Chat_Test 已完成進度 [' in line]
        ordinary=[line for line in lines if '已完成進度 [初見殺]。' in line]
        hidden=[line for line in lines if '已完成進度 [大家可以回家啦！]。' in line]
        if len(lines)!=2 or len(ordinary)!=1 or len(hidden)!=1:
            raise AssertionError(f'Expected one chat announcement per ordinary/hidden event, found {lines}')
        if ('新手教學的最後一課：你會死。' not in ordinary[0] or '累計死亡 1 次。' in ordinary[0]
                or '工程隊全員失業了' not in hidden[0] or '生存模式下雙手皆空，成功拆除一個混凝土方塊。' in hidden[0]
                or '墓碑負責記錄失敗的草稿' in log):
            raise AssertionError('Expected flavor-only introductions on the same logical chat line and no root announcement')
        print('PASS: one chat entry per ordinary/hidden achievement; full title and flavor only; root, other mods and disabled gamerule excluded.',flush=True)
        print(f'PASS: actual NeoForge server loaded {EXPECTED} ZoneZero advancement definitions, including root.',flush=True)
        print('RCON list:',result,flush=True)
        packs=rcon('datapack list enabled')
        print('Enabled data packs:',packs,flush=True)
        if 'zonezero' not in packs.lower() and 'mod' not in packs.lower():
            raise AssertionError('Expected mod datapack not listed')
        before=text().count('ZoneZero: applied compact coordinates to 203 nodes.')
        rcon('reload')
        deadline=time.monotonic()+60
        while text().count('ZoneZero: applied compact coordinates to 203 nodes.')<=before:
            if time.monotonic()>=deadline:raise AssertionError('Compact layout missing after datapack reload')
            time.sleep(1)
        print('PASS: compact positions also applied after datapack reload.',flush=True)
        rcon('stop',shutdown=True)
        if proc.wait(timeout=90)!=0:raise RuntimeError('Gradle server run returned an error')
        log=text()
        if 'ZoneZero: root advancement is missing' in log or re.search(r'(Failed|Couldn.t) to (parse|load) (?:advancement[^\n]*zonezero_advancements|zonezero_advancements[^\n]*)',log,re.I):
            raise AssertionError('ZoneZero data loading error')
        print('PASS: server stopped cleanly; no missing-root or ZoneZero advancement parsing error.',flush=True)
    except BaseException:
        print('SMOKE TEST FAILED — last console lines:',flush=True)
        # Limit log output and redact the temporary RCON password if present.
        print('\n'.join(text().replace(password,'[redacted]').splitlines()[-120:]),flush=True)
        if proc.poll() is None:
            os.killpg(proc.pid,signal.SIGTERM)
            try:proc.wait(timeout=15)
            except subprocess.TimeoutExpired:
                os.killpg(proc.pid,signal.SIGKILL);proc.wait()
        raise
