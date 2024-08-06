package it.metro.centers;

import it.metro.events.Event;
import it.metro.utils.Area;
import it.metro.utils.Rngs;
import it.metro.utils.Rvgs;

public abstract class Center {

    final int ID;                       //identificativo univoco del centro
    public int numServer;               //numero di servers nel centro
    public int numJobs = 0;             //numero di jobs attualmente nel centro (servers + coda)
    public int completedJobs = 0;       //numero di jobs processati dal server
    public Event currentEvent;          //l'evento correntemente processato dal centro
    public Area area;
    public Event lastArrival;
    public Rvgs r;

    public Center(int id, int numServer, Rvgs r) {
        ID = id;
        this.numServer = numServer;     //inizializza il numero di server per il centro
        this.area = new Area();         //inizializza a zero le statistiche del centro
        this.r = r;
    }

    //ritorna il centro successivo (il raggiungimento del centro successivo può essere probabilistico)
    public abstract Center getNextCenter();

    public abstract double getService();

    //processa l'arrivo di un job nel centro
    public void processArrival() {
        numJobs += 1;

        //se è disponibile un server
        if (numJobs <= numServer) {
            getService();
            //TODO
        }
    };

    //processa il completamento di un job nel centro
    public abstract void processDeparture();

    //aggiorna le statistiche per il centro
    public void updateStatistics(Event newEvent) {
        if (numJobs > 0) {
            double delta = (newEvent.getTime() - currentEvent.getTime());
            area.node    += delta * numJobs;
            area.queue   += delta * (numJobs - 1);
            area.service += delta;
        }

        //aggiorna l'evento corrente per il centro
        currentEvent = newEvent;
    }
}
