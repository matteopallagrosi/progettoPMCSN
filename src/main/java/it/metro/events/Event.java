package it.metro.events;

public class Event {

    private EventType type;     //tipologia dell'evento (arrivo, completamento...)
    private double time;        //tempo di simulazione in cui avviene l'evento

    public Event(EventType type, double time) {
        this.type = type;
        this.time = time;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }
}
