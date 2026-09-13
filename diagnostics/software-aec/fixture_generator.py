#!/usr/bin/env python3
"""Generate deterministic render/capture PCM16 fixtures for SteveApm."""
import argparse, math, struct, wave
from pathlib import Path

def main():
    p=argparse.ArgumentParser(); p.add_argument('out',type=Path); p.add_argument('--seconds',type=float,default=12); p.add_argument('--rate',type=int,default=16000); p.add_argument('--delay-ms',type=int,default=80); p.add_argument('--echo-db',type=float,default=-12); a=p.parse_args()
    n=int(a.seconds*a.rate); delay=round(a.delay_ms*a.rate/1000); gain=10**(a.echo_db/20)
    render=[]; desired=[]; capture=[]
    for i in range(n):
        t=i/a.rate
        # Deterministic speech-like multi-band excitation with slow envelope.
        env=0.55+0.45*math.sin(2*math.pi*0.7*t)**2
        x=env*(0.28*math.sin(2*math.pi*173*t)+0.18*math.sin(2*math.pi*311*t)+0.11*math.sin(2*math.pi*587*t))
        d=0.16*math.sin(2*math.pi*97*t)+0.07*math.sin(2*math.pi*233*t+0.4)
        e=(render[i-delay]*gain) if i>=delay else 0
        render.append(x); desired.append(d); capture.append(x*0+ d+e)
    a.out.mkdir(parents=True,exist_ok=True)
    def write(name, vals): (a.out/(name+'.pcm')).write_bytes(struct.pack('<%dh'%len(vals),*[max(-32768,min(32767,round(v*28000))) for v in vals]))
    write('render',render); write('capture',capture); write('desired',desired)
    (a.out/'metadata.txt').write_text(f'rate={a.rate}\nchannels=1\nseconds={a.seconds}\nframe_samples={a.rate//100}\ndelay_ms={a.delay_ms}\necho_db={a.echo_db}\necho_gain={gain}\nrender_peak=0.57\ndesired_peak=0.23\n')
if __name__=='__main__': main()
