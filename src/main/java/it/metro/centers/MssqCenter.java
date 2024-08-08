package it.metro.centers;

import it.metro.events.Event;
import it.metro.utils.Area;
import it.metro.utils.Rvgs;
import it.metro.utils.Server;
import it.metro.utils.Time;

import java.text.DecimalFormat;

//Multi Server Single Queue Center
public abstract class MssqCenter extends Center {

    public MssqCenter(int id, int numServer, Rvgs v) {
        super(id, numServer, v);
        this.area = new Area[1];
        this.area[0] = new Area();
    }

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
            area[0].node    += delta * numJobs;
            area[0].queue   += delta * (numJobs - numBusyServers);
            area[0].service += delta;
        }

        //aggiorna l'evento corrente per il centro
        currentEvent = newEvent;
    }

    public void printStatistics(Time t) {
        DecimalFormat f = new DecimalFormat("###0.00");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("\nfor " + completedJobs + " jobs the service node statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(t.last / completedJobs));
        System.out.println("  avg wait ........... =   " + f.format(area[0].node / completedJobs));
        System.out.println("  avg # in node ...... =   " + f.format(area[0].node / t.current));
        System.out.println("  avg delay .......... =   " + f.format(area[0].queue / completedJobs));
        System.out.println("  avg # in queue ..... =   " + f.format(area[0].queue / t.current));


        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (int s = 0; s < numServer; s++) {
            System.out.print("       " + s + "          " + g.format(servers[s].service / t.current) + "            ");
            System.out.println(f.format(servers[s].service / servers[s].served) + "         " + g.format(servers[s].served / (double)completedJobs));
        }
        //share = percentuale di job processati da quel server sul totale

        System.out.println("");
    }
}
