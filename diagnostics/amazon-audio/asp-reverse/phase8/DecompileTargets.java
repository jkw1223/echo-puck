import ghidra.app.decompiler.*;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Function;

public class DecompileTargets extends GhidraScript {
  public void run() throws Exception {
    DecompInterface d = new DecompInterface();
    d.openProgram(currentProgram);
    long[] addrs={0x566a4L,0x56aecL,0x57b44L,0x8ac14L,0x8b050L,0x8b734L,0x8ba50L,0x90fa4L,0x8d6f8L,0x575e0L,0x57e38L,0x8b360L,0x8c464L,0x9f830L,0x84a90L};
    for(long a:addrs){
      Function f=currentProgram.getFunctionManager().getFunctionAt(toAddr(a));
      if(f==null){println("MISSING 0x"+Long.toHexString(a)); continue;}
      DecompileResults r=d.decompileFunction(f,120,monitor);
      println("===== FUNCTION "+f.getName()+" "+f.getEntryPoint()+" =====");
      println(r.getDecompiledFunction()==null?"DECOMPILER_ERROR "+r.getErrorMessage():r.getDecompiledFunction().getC());
    }
    d.dispose();
  }
}
