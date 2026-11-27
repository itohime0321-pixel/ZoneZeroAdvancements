package zonezero.advancements;

import com.mojang.logging.LogUtils;
import net.minecraft.advancements.AdvancementHolder;
import java.io.IOException;
import java.util.*;

/** Stable slots generated from the same definitions as the achievement data. */
public final class CompactAdvancementLayout {
    private static final String NAMESPACE = "zonezero_advancements";
    private static final Map<String, Point> SLOTS = load();
    private CompactAdvancementLayout() {}
    private record Point(int x, int y) {}
    private static Map<String, Point> load() {
        Properties properties = new Properties();
        try (var in = CompactAdvancementLayout.class.getResourceAsStream("/zonezero_advancements_layout.properties")) {
            if (in == null) throw new IOException("Missing achievement layout resource");
            properties.load(in);
        } catch (IOException e) {
            LogUtils.getLogger().error("ZoneZero: could not load compact layout; retaining vanilla positions.", e);
            return Map.of();
        }
        Map<String, Point> points = new HashMap<>();
        for (String path : properties.stringPropertyNames()) {
            String[] xy = properties.getProperty(path).split(",");
            points.put(path, new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
        }
        return Map.copyOf(points);
    }
    /** Runs after vanilla places the tree, both at startup and datapack reload. */
    public static void apply(Collection<AdvancementHolder> advancements) {
        int count = 0;
        for (var holder : advancements) {
            if (!NAMESPACE.equals(holder.id().getNamespace())) continue;
            Point point = SLOTS.get(holder.id().getPath());
            var display = holder.value().display();
            if (point != null && display.isPresent()) {
                display.get().setLocation(point.x(), point.y());
                count++;
            }
        }
        LogUtils.getLogger().info("ZoneZero: applied compact coordinates to {} nodes.", count);
    }
    public static void verify(Collection<AdvancementHolder> advancements) {
        Set<Point> occupied = new HashSet<>();
        for (var holder : advancements) {
            if (!NAMESPACE.equals(holder.id().getNamespace())) continue;
            Point point = SLOTS.get(holder.id().getPath());
            var display = holder.value().display().orElseThrow();
            if (point == null || point.x() != display.getX() || point.y() != display.getY()
                    || !occupied.add(point))
                throw new IllegalStateException("Incorrect compact layout for " + holder.id());
        }
        if (occupied.size() != SLOTS.size() || occupied.size() != 203)
            throw new IllegalStateException("Incomplete compact layout: " + occupied.size());
        LogUtils.getLogger().info("ZoneZero test: verified all 203 unique compact positions.");
    }
}
