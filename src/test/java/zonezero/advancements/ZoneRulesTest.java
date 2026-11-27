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
        if (ZoneRules.fullMoonNight(12999) || !ZoneRules.fullMoonNight(13000)
                || !ZoneRules.fullMoonNight(22999) || ZoneRules.fullMoonNight(23000)
                || ZoneRules.fullMoonNight(24000+18000)
                || !ZoneRules.fullMoonNight(8*24000L+18000)) throw new AssertionError("full moon boundaries");
        var burst=new java.util.ArrayDeque<Long>();
        for (long t:new long[]{0,50,100,150}) if (ZoneRules.addBurstKill(burst,t)) throw new AssertionError("early burst");
        if (!ZoneRules.addBurstKill(burst,200)) throw new AssertionError("inclusive 200 tick burst");
        burst.clear();
        for (long t:new long[]{0,50,100,150,201}) if (ZoneRules.addBurstKill(burst,t)) throw new AssertionError("expired burst");
        if (!ZoneRules.addBurstKill(burst,220)) throw new AssertionError("rolling burst");
        if (!ZoneRules.newEffectDuration(0,6000) || !ZoneRules.newEffectDuration(1,6000)
                || ZoneRules.newEffectDuration(6000,6000) || ZoneRules.newEffectDuration(6000,0)
                || ZoneRules.newEffectDuration(-1,6000) || !ZoneRules.newEffectDuration(6000,-1))
            throw new AssertionError("new thrombosis effect or duration extension");
        if (!ZoneRules.confirmedHeadshot(true,true,true,true,true,true)) throw new AssertionError("headshot success");
        for (int i=0;i<6;i++) {
            boolean[] v={true,true,true,true,true,true};v[i]=false;
            if (ZoneRules.confirmedHeadshot(v[0],v[1],v[2],v[3],v[4],v[5])) throw new AssertionError("headshot false positive "+i);
        }
        if (!ZoneRules.confirmedRevive(true,false,true,true) || ZoneRules.confirmedRevive(false,false,true,true)
                || ZoneRules.confirmedRevive(true,true,true,true) || ZoneRules.confirmedRevive(true,false,false,true)
                || ZoneRules.confirmedRevive(true,false,true,false)) throw new AssertionError("revive success/failure");
        if (!ZoneRules.concreteBreak(true,true,true,true,true,false)) throw new AssertionError("successful empty-hand concrete break");
        for (int i=0;i<6;i++) {
            boolean[] v={true,true,true,true,true,false};
            v[i]=!v[i];
            if (ZoneRules.concreteBreak(v[0],v[1],v[2],v[3],v[4],v[5]))
                throw new AssertionError("concrete false positive "+i);
        }
        if (ZoneRules.longRangeArrow(29.999*29.999) || !ZoneRules.longRangeArrow(900)
                || !ZoneRules.longRangeArrow(2500) || ZoneRules.longRangeArrow(Double.NaN))
            throw new AssertionError("arrow distance boundary");
        System.out.println("PASS: seven radial boundaries and fresh skill point baselines, full moon nights and rolling bursts, confirmed headshots and completed rescues, empty-hand concrete breaks and arrow distance");
    }
}
