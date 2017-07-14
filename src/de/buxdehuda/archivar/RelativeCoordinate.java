package de.buxdehuda.archivar;

public class RelativeCoordinate {
    
    double x;
    double y;

    public RelativeCoordinate(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
    
    public RelativeCoordinate(String serialized) {
        int i = serialized.indexOf('/');
        this.x = Double.parseDouble(serialized.substring(0, i));
        this.y = Double.parseDouble(serialized.substring(i + 1));
    }
    
    @Override
    public String toString() {
        return this.x + "/" + this.y;
    }
    
}
