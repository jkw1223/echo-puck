import ghidra.app.script.GhidraScript;
import ghidra.program.model.mem.Memory;
public class DumpBytes extends GhidraScript {
 public void run() throws Exception { long[] a={0x57cd0L,0x57cf0L,0x57de0L,0x8b8fcL,0x8b9e0L,0x8ba14L,0xce606L,0xec61cL,0xef3d8L}; Memory m=currentProgram.getMemory(); for(long x:a){byte[] b=new byte[32]; try{m.getBytes(toAddr(x),b);}catch(Exception e){} StringBuilder s=new StringBuilder(); for(byte q:b)s.append(String.format("%02x",q&255)); println(String.format("BYTES 0x%08x %s",x,s)); } }
}
