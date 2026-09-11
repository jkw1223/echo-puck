from collect import *
import re,hashlib
paths=['/system/lib/libasp.so','/system/lib/libaspclient.so','/vendor/lib/libasp.so','/vendor/lib/libaspclient.so','/vendor/lib/hw/audio.primary_amazon.mt8163.so','/vendor/lib/hw/audio.primary.mt8163.so','/vendor/lib/libspeech_enh_lib.so','/vendor/lib/libaudiocomponentengine.so','/vendor/lib/libaudiotoolkit.so','/vendor/lib/libaudiosetting.so']
shell('specific-paths.txt','; '.join('ls -l '+shlex.quote(p) for p in paths))
run('pull-algorithms.txt',ADB+['pull','/vendor/etc/audio-algorithms',str(ROOT/'audio-algorithms')])
configs=['/vendor/etc/audio_device.xml','/system/etc/audio_device.xml','/vendor/etc/audio_em.xml','/vendor/etc/audio_param/AudioParamOptions.xml','/vendor/etc/audio_policy_configuration.xml','/system/etc/audio_policy_configuration.xml']
for p in configs:
 dest=ROOT/'configs'/p.lstrip('/');dest.parent.mkdir(parents=True,exist_ok=True)
 run('pull-'+p.strip('/').replace('/','_')+'.txt',ADB+['pull',p,str(dest)])
files=(ROOT/'audio-files.txt').read_text().splitlines()
libs=[p for p in files if re.match(r'(audio\.primary|libasp|libspeech_enh|libaudiocomponentengine|libaudiotoolkit|libaudiosetting)',Path(p).name)]
pat=re.compile(r'aec|echo|reference|beam|fbf|mic|array|speech|enhance|noise|\bns\b|agc|voice|voip|recognition|hotword|keyword|wake|trigger|source|input|capture|parameter|routing',re.I)
for p in libs:
 dest=ROOT/'libraries'/p.lstrip('/');dest.parent.mkdir(parents=True,exist_ok=True)
 label=p.strip('/').replace('/','_')
 run('pull-'+label+'.txt',ADB+['pull',p,str(dest)])
 if dest.exists():
  s=run(label+'-strings-all.txt',['/usr/bin/strings','-a',str(dest)])
  (ROOT/(label+'-strings-interesting.txt')).write_text('\n'.join(x for x in s.splitlines() if pat.search(x))+'\n')
shell('soundtrigger-middleware.txt','dumpsys soundtrigger_middleware')
shell('soundtrigger-manifests.txt',"grep -riE 'sound.?trigger|hotword' /vendor/etc/vintf /system/etc/vintf /vendor/etc/init /system/etc/init 2>/dev/null")
shell('loaded-audio-libraries.txt',"for p in $(pidof audioserver android.hardware.audio@2.0-service); do echo PID=$p; cat /proc/$p/maps | grep -Ei 'audio|asp|speech|sound|effect'; done")
(ROOT/'SHA256SUMS').write_text(''.join(hashlib.sha256(f.read_bytes()).hexdigest()+'  '+str(f.relative_to(ROOT))+'\n' for d in ['libraries','configs','audio-algorithms'] for f in sorted((ROOT/d).rglob('*')) if f.is_file()))
