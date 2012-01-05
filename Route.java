/**
 * Represents a route from one tile to another.
 */
public class Route {
    public final Tile start;
    public final Tile end;
    public static final int MAX_MAP_SIZE_2 = Game.MAX_MAP_SIZE * Game.MAX_MAP_SIZE;

    public Route(Tile start, Tile end) {
        this.start = start;
        this.end = end;
    }

    public Tile getStart() {
        return start;
    }

    public Tile getEnd() {
        return end;
    }

    @Override
    public int hashCode() {
        return start.hashCode() * MAX_MAP_SIZE_2 + end.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if (o instanceof Route) {
            Route route = (Route)o;
            result = start == route.start && end == route.end;
        }
        return result;
    }

    @Override
    public String toString() {
        return start.toString() + " -> " + end.toString();
    }    
}