from pathlib import Path
import subprocess,datetime,json,shlex
ROOT=Path(__file__).resolve().parent
ADB=['/Users/jason/Library/Android/sdk/platform-tools/adb','-s','192.168.1.24:5555']
def run(name,args):
 t=datetime.datetime.now(datetime.timezone.utc).isoformat()
 try:
  r=subprocess.run(args,capture_output=True,timeout=60);rc=r.returncode;out=r.stdout;err=r.stderr
 except subprocess.TimeoutExpired as e:rc=124;out=e.stdout or b'';err=(e.stderr or b'')+b'\nTIMEOUT\n'
 (ROOT/name).write_bytes(out);(ROOT/(name+'.stderr')).write_bytes(err)
 with (ROOT/'commands.jsonl').open('a') as f:f.write(json.dumps({'time':t,'command':shlex.join(args),'exit_code':rc,'stdout':name,'stderr':name+'.stderr'})+'\n')
 return out.decode(errors='replace')
def shell(name,cmd):return run(name,ADB+['shell',cmd])
if __name__=='__main__':
 shell('device.txt','id; getenforce; getprop ro.product.device; getprop ro.product.model; getprop ro.build.fingerprint; date; pidof com.steve.puckd')
 shell('audio-files.txt',"find /system /vendor -type f \\( -iname '*audio*' -o -iname '*speech*' -o -iname '*asp*' -o -iname '*sound*trigger*' \\) 2>/dev/null")
 shell('audio-config-files.txt',"find /vendor/etc /system/etc -type f 2>/dev/null | grep -Ei 'audio|speech|aec|beam|voice|asp|hotword|sound.?trigger'")
 shell('audio-algorithms-list.txt','ls -la /vendor/etc/audio-algorithms/')
 shell('hotword-files.txt',"find /system /vendor -type f 2>/dev/null | grep -Ei 'sound.?trigger|hotword|voice.?trigger|keyword|wakeword'")
 shell('services-audio-hotword.txt',"service list | grep -Ei 'sound|voice|hotword|audio'")
 for service in ['media.sound_trigger_hw','soundtrigger','voiceinteraction']:
  shell('dumpsys-'+service+'.txt','dumpsys '+service)
 shell('audio-properties.txt',"getprop | grep -Ei 'audio|sound|voice|speech|amazon|mtk|hotword|wake'")
 shell('hardware-services.txt','lshal')
 shell('audioflinger-initial.txt','dumpsys media.audio_flinger')
 shell('audiopolicy-initial.txt','dumpsys media.audio_policy')
