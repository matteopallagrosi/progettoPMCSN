package it.metro.centers;

import it.metro.events.Event;
import it.metro.utils.Area;
import it.metro.utils.Rvgs;
import it.metro.utils.Server;

public abstract class Center {

    final int ID;                       //identificativo univoco del centro
    public int numServer;               //numero di servers nel centro
    public int numJobs = 0;             //numero di jobs attualmente nel centro (servers + coda)
    public int completedJobs = 0;       //numero di jobs processati dal server
    public Event currentEvent;          //l'evento correntemente processato dal centro
    public Area area;
    public Event lastArrival;
    public double lastService;          //ultimo tempo di servizio generato
    public Rvgs r;
    public Server[] servers;            //lista di server del centro
    public int numBusyServers = 0;      //numero di server occupati del centro


    public Center(int id, int numServer, Rvgs r) {
        ID = id;
        this.numServer = numServer;     //inizializza il numero di server per il centro
        this.area = new Area();         //inizializza a zero le statistiche del centro
        this.r = r;
        this.servers = new Server[numServer];
        for (int i = 0; i < numServer; i++) {
            this.servers[i] = new Server();
        }
    }

    //ritorna il centro successivo (il raggiungimento del centro successivo può essere probabilistico)
    public abstract Center getNextCenter();

    public abstract double getService();

    //processa l'arrivo di un job nel centro
    public int processArrival() {
        numJobs += 1;

        //se è disponibile un server, il job viene immediatamente servito
        //ritorna l'indice del server per cui deve essere prodotto un tempo di completamento, altrimenti -1 se job va in coda
        if (numJobs <= numServer) {
            lastService = getService();
            int serverIndex = findIdleServer();
            Server selectedServer = servers[serverIndex];
            selectedServer.service += lastService;
            selectedServer.served++;
            selectedServer.idle = false;
            numBusyServers++;
            //il controller produce un evento di completamento
            return serverIndex;
        }
        return -1;  //nel caso in cui non deve essere prodotto un evento di completamento ritorna -1
    };

    //processa il completamento di un job nel centro
    //ritorna valore != -1 se è presente un ulteriore job in coda da processare
    public int processDeparture() {
        completedJobs += 1;
        numJobs -= 1;
        //aggiorna l'ultimo completamento presso il server coinvolto (necessario per selezionare il server libero da più tempo)
        currentEvent.getServer().lastDeparture = currentEvent.getTime();
        //se è presente almeno un job in coda, questo viene prelevato e mandato in servizio nel server appena liberato
        if (numJobs >= numServer) {
            lastService = getService();
            currentEvent.getServer().service += lastService;
            currentEvent.getServer().served++;
            //il controller produce un evento di completamento
            return 0;
        }
        //altrimenti il server torna a essere libero
        else {
            currentEvent.getServer().idle = true;
            numBusyServers--;
        }
        return -1;
    }

    //aggiorna le statistiche per il centro
    public void updateStatistics(Event newEvent) {
        if (numJobs > 0) {
            double delta = (newEvent.getTime() - currentEvent.getTime());
            area.node    += delta * numJobs;
            area.queue   += delta * (numJobs - numBusyServers);
            area.service += delta;
        }

        //aggiorna l'evento corrente per il centro
        currentEvent = newEvent;
    }

    //quando questa funzione viene invocata, è già stata controllata la presenza di almeno un server libero
    //seleziona tra i server liberi, quello libero da più tempo
    private int findIdleServer() {
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
}
