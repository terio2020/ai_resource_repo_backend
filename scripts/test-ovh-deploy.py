#!/usr/bin/env python3
"""Exercise the remote deployment transaction in an isolated fake host (no SSH)."""
import os
from pathlib import Path
import re
import subprocess
import tarfile
import tempfile
import unittest
import hashlib
import io

SCRIPT = Path(__file__).with_name('deploy-ovh.sh')
BACKEND = 'OVH backend' in SCRIPT.read_text()
REMOTE = re.findall(r"<<'REMOTE'\n(.*?)\nREMOTE", SCRIPT.read_text(), re.S)[-1]


class DeployTransactionTest(unittest.TestCase):
    def run_transaction(self, failure=''):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            base = root / 'opt' / ('logicomanet-be' if BACKEND else 'logicomanet-fe')
            base.mkdir(parents=True)
            (root / 'opt/logicomanet').mkdir()
            upload = root / 'upload'
            upload.mkdir()
            if BACKEND:
                (base / 'app.jar').write_bytes(b'old jar')
                (upload / 'app.jar').write_bytes(b'new jar')
                (upload / 'SHA256SUMS').write_text(hashlib.sha256(b'new jar').hexdigest()+'  app.jar\n')
                (upload / 'versions').write_text('1\n13\n' if failure != 'schema' else '1\n')
            else:
                (base / 'dist').mkdir()
                (base / 'dist/index.html').write_text('old index')
                with tarfile.open(upload / 'dist.tgz', 'w:gz') as tar:
                    info=tarfile.TarInfo('./index.html')
                    info.size=len(b'new index')
                    tar.addfile(info, io.BytesIO(b'new index'))
                digest = '0'*64 if failure == 'checksum' else hashlib.sha256(b'new index').hexdigest()
                (upload / 'SHA256SUMS').write_text(digest+'  ./index.html\n')
            bindir=root/'bin'
            bindir.mkdir()
            docker=bindir/'docker'
            docker.write_text('''#!/usr/bin/env python3
import os,sys
from pathlib import Path
args=' '.join(sys.argv[1:])
p=Path(os.environ['MOCK_ROOT'])
with (p/'docker.log').open('a') as f: f.write(args+'\\n')
if 'SELECT version' in args: print('1\\n13')
if 'mysqldump' in args:
    if os.environ.get('FAILURE') == 'dump': sys.exit(7)
    for i in range(20000): print('INSERT INTO example VALUES (%d);'%i)
if 'up -d' in args and os.environ.get('FAILURE') == 'restart' and not (p/'failed-once').exists():
    (p/'failed-once').touch()
    sys.exit(8)
''')
            docker.chmod(0o755)
            curl=bindir/'curl'
            curl.write_text('''#!/bin/sh
[ "$FAILURE" != health ] || exit 22
printf '%s' '{"status":"UP"}'
''')
            curl.chmod(0o755)
            # The remote host is Linux; provide GNU stat's size flag on macOS.
            stat=bindir/'stat'
            stat.write_text('#!/usr/bin/env python3\nimport os,sys\nprint(os.stat(sys.argv[2]).st_size)\n')
            stat.chmod(0o755)
            sleep=bindir/'sleep'
            sleep.write_text('#!/bin/sh\nexit 0\n')
            sleep.chmod(0o755)
            script=REMOTE.replace('/opt/logicomanet',str(root/'opt/logicomanet'))
            result=subprocess.run(['bash','-s','--',str(upload),'false' if BACKEND else 'true'],input=script,text=True,capture_output=True,env={**os.environ,'PATH':str(bindir)+':'+os.environ['PATH'],'MOCK_ROOT':tmp,'FAILURE':failure})
            log=(root/'docker.log').read_text() if (root/'docker.log').exists() else ''
            artifact=(base/'app.jar').read_text() if BACKEND else (base/'dist/index.html').read_text()
            backups=list((root/'opt/logicomanet/backups').glob('*.gz'))
            modes=[p.stat().st_mode & 0o777 for p in backups]
            return result,log,artifact,modes

    def test_success(self):
        result,log,artifact,modes=self.run_transaction()
        self.assertEqual(result.returncode,0,result.stderr)
        self.assertEqual(artifact,'new jar' if BACKEND else 'new index')
        self.assertIn('restart backend' if BACKEND else '--force-recreate --pull never nginx',log)
        if BACKEND: self.assertEqual(modes,[0o600])

    def test_failure_gate(self):
        result,log,artifact,_=self.run_transaction('dump' if BACKEND else 'checksum')
        self.assertNotEqual(result.returncode,0)
        self.assertEqual(artifact,'old jar' if BACKEND else 'old index')
        self.assertNotIn('restart backend' if BACKEND else 'up -d',log)

    def test_recovery_boundary(self):
        result,log,artifact,_=self.run_transaction('health' if BACKEND else 'restart')
        self.assertNotEqual(result.returncode,0)
        if BACKEND:
            self.assertEqual(artifact,'new jar')
            self.assertIn('No automatic JAR or schema downgrade',result.stderr)
        else:
            self.assertEqual(artifact,'old index')
            self.assertEqual(log.count('up -d'),2)

    @unittest.skipUnless(BACKEND,'backend schema gate')
    def test_old_jar_blocked_before_backup_and_restart(self):
        result,log,artifact,modes=self.run_transaction('schema')
        self.assertNotEqual(result.returncode,0)
        self.assertEqual(artifact,'old jar')
        self.assertNotIn('restart backend',log)
        self.assertNotIn('mysqldump',log)
        self.assertEqual(modes,[])

    def test_local_dry_run_and_retired_aws(self):
        entry=SCRIPT.parent.parent/'deploy.sh'
        for args,expected in [(['--dry-run'],0),(['--target=aws'],1)]:
            result=subprocess.run(['bash',str(entry),*args],text=True,capture_output=True,env={**os.environ,'LOGICOMA_SSH_KEY':'/nonexistent'})
            self.assertEqual(result.returncode,expected,result.stderr)


if __name__ == '__main__': unittest.main()
