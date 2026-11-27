package zonezero.advancements;

/** Pure rules shared by tracking and test code. No mutation of the host's gameplay. */
public final class ZoneRules {
    private ZoneRules() {}
    public static final int[] DEATHS = {1,10,30,50,75,100,150,200,250,300,350,400,450,486,500,550,600,650,700,750,800,850,900,950,1000};
    public static final int[] GENETICS = {1,10,20,30,40,46};
    public static final int[] GUNS = {1,10,20,30,40,50,60,70,80,90,93};
    public static final int[] ECONOMY = {1,100,500,1000,5000,10000,50000,100000};
    private static final int[] RADII = {400,800,1200,1600,2000,2400,3001};

    // Return zero outside the seven player-visible rings, including invalid positions.
    public static int zone(double x, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(z)) return 0;
        double d2 = x*x + z*z;
        for (int i=0;i<RADII.length;i++) {
            double r = RADII[i];
            if (d2 <= r*r) return i+1;
        }
        return 0;
    }
    public static int newProgress(int initial, int current) {
        return Math.max(0,current - Math.max(0,initial));
    }
}
