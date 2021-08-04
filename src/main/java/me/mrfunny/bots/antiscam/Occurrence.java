package me.mrfunny.bots.antiscam;

public class Occurrence {
    private long lastOccurrence = System.currentTimeMillis();
    private final String id;

    public Occurrence(String id) {
        this.id = id;
    }

    public void addOccurrence(){
        lastOccurrence = System.currentTimeMillis();
    }

    public long getLastOccurrence() {
        return lastOccurrence;
    }

    public String getId() {
        return id;
    }
}
