package zonezero.advancements;

/** No Minecraft boot required; runs through Gradle check. */
public final class ZoneRulesTest {
    private static void eq(int actual,int expected) {
        if(actual!=expected) throw new AssertionError(actual+" != "+expected);
    }
    public static void main(String[] args) {
        eq(ZoneRules.zone(0,0),1);
        int[] boundaries={400,800,1200,1600,2000,2400,3001};
        for(int i=0;i<boundaries.length;i++) {
            eq(ZoneRules.zone(-boundaries[i],0),i+1);
            eq(ZoneRules.zone(0,boundaries[i]),i+1);
            eq(ZoneRules.zone(boundaries[i]+0.01,0),i==6?0:i+2);
        }
        eq(ZoneRules.zone(300,300),2);
        eq(ZoneRules.zone(-300,-300),2);
        eq(ZoneRules.zone(Double.NaN,0),0);
        eq(ZoneRules.newProgress(20,20),0);
        eq(ZoneRules.newProgress(20,30),10);
        eq(ZoneRules.newProgress(20,10),0);
        eq(ZoneRules.newProgress(0,46),46);
        eq(ZoneRules.newProgress(0,93),93);
        System.out.println("PASS: seven radial boundaries and fresh skill point baselines");
    }
}
