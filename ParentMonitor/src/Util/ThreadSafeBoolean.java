package Util;

public final class ThreadSafeBoolean {

    private final Object lock = new Object();
    private boolean value;
    
    public ThreadSafeBoolean(boolean v) {
        value = v;
    }

    public boolean get() {
        synchronized (lock) {
            return value;
        }
    }

    public void set(boolean v) {
        synchronized (lock) {
            value = v;
        }
    }
    
    public void invert() {
        synchronized (lock) {
            value = !value;
        }
    }
    
    @Override
    public String toString() {
        synchronized (lock) {
            return value ? "true" : "false";
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ThreadSafeBoolean)) {
            return false;
        }
        synchronized (lock) {
            return value == ((ThreadSafeBoolean) obj).value;
        }
    }

    @Override
    public int hashCode() {
        synchronized (lock) {
            return value ? 1 : 0;
        }
    }
}