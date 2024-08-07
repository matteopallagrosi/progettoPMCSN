package it.metro.events;

import it.metro.centers.Center;
import it.metro.utils.Server;

public class Event {

    private EventType type;     //tipologia dell'evento (arrivo, completamento...)
    private double time;        //tempo di simulazione in cui avviene l'evento
    private Center center;      //centro a cui è destinato l'evento
    private Server server;      //server di quel centro a cui è destinato l'evento (in caso di completamento)

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

    public Center getCenter() {
        return center;
    }

    public void setCenter(Center center) {
        this.center = center;
    }


    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }
}
