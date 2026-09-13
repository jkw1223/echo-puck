#!/usr/bin/env python3
import argparse, struct, math
def read(p): return struct.unpack('<%dh'%(p.stat().st_size//2),p.read_bytes())
def dot(a,b): return sum(x*y for x,y in zip(a,b))
def rms(a): return math.sqrt(sum(x*x for x in a)/len(a))
def main():
 p=argparse.ArgumentParser(); p.add_argument('render'); p.add_argument('before'); p.add_argument('after'); p.add_argument('--delay',type=int,default=1280); a=p.parse_args()
 r,b,o=map(lambda x:read(__import__('pathlib').Path(x)),[a.render,a.before,a.after]); rr=r[a.delay:]; bb=b[a.delay:]; oo=o[a.delay:]
 def corr(x): return dot(x,rr)/math.sqrt(dot(x,x)*dot(rr,rr))
 eb=dot(bb,rr)**2/dot(rr,rr); ea=dot(oo,rr)**2/dot(rr,rr)
 print(f'before_rms={rms(bb):.3f}\nafter_rms={rms(oo):.3f}\nbefore_correlation={corr(bb):.6f}\nafter_correlation={corr(oo):.6f}\nbefore_correlated_energy={eb:.3f}\nafter_correlated_energy={ea:.3f}\necho_reduction_db={10*math.log10(eb/ea) if ea else float("inf"):.3f}')
if __name__=='__main__': main()
