package it.metro.centers;

import it.metro.events.Event;
import it.metro.utils.Rvgs;
import it.metro.utils.Server;
import it.metro.utils.Time;

import java.text.DecimalFormat;

//Multi Server multi queue Center (like frequency division multiplexing system)
//Per ogni server c'è una coda a lui dedicata (quindi numero code = numServer)
public class MsmqCenter extends Center {

    public int[] queues;            //tiene traccia della popolazione in ogni coda del centro

    public MsmqCenter(int id, int numServer, Rvgs v) {
        super(id, numServer, v);
        queues = new int[numServer];
    }

    @Override
    public Center getNextCenter() {
        return null;
    }

    @Override
    public double getService() {
        return 0;
    }

    public int processArrival() {
        numJobs += 1;

        //se è disponibile un server, il job viene immediatamente servito
        //ritorna l'indice del server per cui deve essere prodotto un tempo di completamento, altrimenti -1 se job va in coda
        if (isSomeServerIdle()) {
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
        //altrimenti il job viene inserito in una delle code secondo la politica di accodamento creata
        int selectedQueue = selectQueue();
        queues[selectedQueue]++;
        return -1;
    }

    @Override
    public int processDeparture() {
        completedJobs += 1;
        numJobs -= 1;
        //aggiorna l'ultimo completamento presso il server coinvolto (necessario per selezionare il server libero da più tempo)
        currentEvent.getServer().lastDeparture = currentEvent.getTime();
        //se è presente almeno un job nella coda del server in cui c'è stato il completamento, questo viene prelevato e mandato in servizio in questo server
        if (queues[currentEvent.getServer().id] > 0) {
            queues[currentEvent.getServer().id]--;
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

    @Override
    public void updateStatistics(Event newEvent) {
        int currentQueue = newEvent.getServer().id;
        int jobsInService = (newEvent.getServer().idle) ?  1 : 0;
        if ((queues[currentQueue] + jobsInService) > 0) {
            double delta = (newEvent.getTime() - currentEvent.getTime());
            area[currentQueue].node    += delta * (queues[currentQueue] + jobsInService);
            area[currentQueue].queue   += delta * (queues[currentQueue]);
            area[currentQueue].service += delta;
        }

        //aggiorna l'evento corrente per il centro
        currentEvent = newEvent;
    }

    @Override
    public void printStatistics(Time t) {
        DecimalFormat f = new DecimalFormat("###0.00");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("\nfor " + completedJobs + " jobs the service node statistics are:\n");

        for (int i = 0; i < numServer; i++) {
            System.out.println("Queue + Server " + i + ":");
            System.out.println("  avg interarrivals .. =   " + f.format(t.last / servers[i].served));
            System.out.println("  avg wait ........... =   " + f.format(area[i].node / servers[i].served));
            System.out.println("  avg # in node ...... =   " + f.format(area[i].node / t.current));
            System.out.println("  avg delay .......... =   " + f.format(area[i].queue / servers[i].served));
            System.out.println("  avg # in queue ..... =   " + f.format(area[i].queue / t.current));
        }

        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (int s = 0; s < numServer; s++) {
            System.out.print("       " + s + "          " + g.format(servers[s].service / t.current) + "            ");
            System.out.println(f.format(servers[s].service / servers[s].served) + "         " + g.format(servers[s].served / (double)completedJobs));
        }
        //share = percentuale di job processati da quel server sul totale

        System.out.println("");
    }

    //sceglie randomicamente una delle code (da definire meglio la politica di accodamento)
    private int selectQueue() {
        v.rngs.selectStream(4);
        return (int)v.equilikely(0, numServer-1);
    }

    //verifica se è presente un server libero
    private boolean isSomeServerIdle() {
        for (int i = 0; i < numServer; i++) {
            if (servers[i].idle) {
                return true;
            }
        }
        return false;
    }
}
