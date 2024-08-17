package it.metro.centers;

import it.metro.events.Event;
import it.metro.utils.Area;
import it.metro.utils.Rvgs;
import it.metro.utils.Server;
import it.metro.utils.Time;

public abstract class Center {

    public final int ID;                       //identificativo univoco del centro
    public int numServer;               //numero di servers nel centro
    public int numJobs = 0;             //numero di jobs attualmente nel centro (servers + coda)
    public int completedJobs = 0;       //numero di jobs processati dal server
    public Event currentEvent;          //l'evento correntemente processato dal centro
    public Area[] area;
    public double lastService;          //ultimo tempo di servizio generato
    public Rvgs v;
    public Server[] servers;            //lista di server del centro
    public int numBusyServers = 0;      //numero di server occupati del centro


    public Center(int id, int numServer, Rvgs v) {
        ID = id;
        this.numServer = numServer;         //inizializza il numero di server per il centro
        this.v = v;
        this.servers = new Server[numServer];
        for (int i = 0; i < numServer; i++) {
            this.servers[i] = new Server(i);
        }
    }

    //ritorna il centro successivo (il raggiungimento del centro successivo può essere probabilistico)
    public abstract int getNextCenter();

    public abstract double getService();

    //processa l'arrivo di un job nel centro
    public abstract int processArrival();

    //processa il completamento di un job nel centro
    //ritorna valore != -1 se è presente un ulteriore job in coda da processare
    public abstract int processDeparture();

    //aggiorna le statistiche per il centro
    public abstract void updateStatistics(Event newEvent);

    public abstract void printStatistics();

    //quando questa funzione viene invocata, è già stata controllata la presenza di almeno un server libero
    //seleziona tra i server liberi, quello libero da più tempo
    protected int findIdleServer() {
        int s;
        int i = 0;
        while (!servers[i].idle) {
            i++;
        }
        s = i;
        i++;
        while (i < numServer) {
            if (servers[i].idle && (servers[i].lastDeparture < servers[s].lastDeparture)) {
                s = i;
            }
            i++;
        }
        return s;
    }

    public abstract double getAvgInterarrival(int i);

    public abstract double getAvgWait(int i);

    public abstract double getAvgDelay(int i);

    public abstract double getAvgNode(int i);

    public abstract double getAvgQueue(int i);

    //ritorna l'utilizzazione dell'i-esimo server del centro
    public abstract double getUtilization(int i);
}
