package Checkout;

public class Checkout {
    private int id;
    private boolean isPrivileged;
    private boolean isFree;

    public Checkout(int id, boolean isPrivileged, boolean isFree) {
        this.id = id;
        this.isPrivileged = isPrivileged;
        this.isFree = isFree;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isPrivileged() {
        return isPrivileged;
    }

    public void setPrivileged(boolean privileged) {
        isPrivileged = privileged;
    }

    public boolean isFree() {
        return isFree;
    }

    public void setFree(boolean free) {
        isFree = free;
    }
}
