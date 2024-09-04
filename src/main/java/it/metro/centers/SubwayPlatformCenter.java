package it.metro.centers;

import it.metro.events.Event;
import it.metro.utils.Area;
import it.metro.utils.Rvgs;

import java.text.DecimalFormat;

//rappresenta il centro della banchina della metro (singola coda, singolo server)
public class SubwayPlatformCenter extends Center {
    public double lastArrive;                      //ultimo arrivo presso questo centro
    final int capacity = 880;                      //rappresenta la capacità della banchina
    public double lastDeparture;
    final int seats = 1200;                        //numero di posti totali sul treno

    public SubwayPlatformCenter(int numServer, Rvgs v) {
        super(6, numServer, v, "Subway Platform Center");
        this.area = new Area[1];
        this.area[0] = new Area();
    }

    //la banchina della metro è il centro finale del sistema, non c'è un centro successivo
    @Override
    public int getNextCenter() {
        return 0;
    }

    @Override
    public double getService() {
        return 0;
    }

    //processo l'arrivo di un utente presso la banchina della metro
    @Override
    public int processArrival() {
        lastArrive = currentEvent.getTime();
        numJobs += 1;
        //System.out.println("persone sulla banchina: " + numJobs);
        //se il numero di utenti ha superato la capacità massima della banchina, viene visualizzato un warning
        //la simulazione viene quindi interrotta
        if (numJobs > capacity) {
            System.out.println("Superata la capacità della banchina!");
            System.exit(0);
        }
        //non viene prodotto alcun evento di completamento
        return -1;
    }

    //All'arrivo del treno i passeggeri lasciano la banchina, quindi escono dal sistema
    @Override
    public int processDeparture() {
        lastDeparture = currentEvent.getTime();
        v.rngs.selectStream(64);
        int busySeats = (int) v.poisson(400);
        int freeSeats = seats - busySeats;
        if (numJobs <= freeSeats) {
            completedJobs += numJobs;
            numJobs = 0;
        }
        else {
            completedJobs += freeSeats;
            numJobs -= freeSeats;
        }
        //System.out.println("persone sulla banchina dopo arrivo del treno: " + numJobs);
        return 0;
    }

    @Override
    public void updateStatistics(Event newEvent) {
        if (numJobs > 0) {
            double delta = (newEvent.getTime() - currentEvent.getTime());
            //rappresenta il tempo di attesa sulla banchina
            //nel caso in cui i treni riescono a prelevare quasi la totalità delle persone ad ogni transito
            //il tempo medio di attesa sarà vicino al tempo medio di interarrivo tra i treni
            //questo perché ogni job riesce a prendere il treno successivo, senza dover attendere il passaggio di un secondo treno
            //se riduco i posti, e/o aumento il tempo di interrarivo tra i treni, mi aspetto che il tempo di attesa aumenti
            area[0].node    += delta * numJobs;
        }

        //aggiorna l'evento corrente per il centro
        currentEvent = newEvent;
    }

    @Override
    public void printStatistics() {
        DecimalFormat f = new DecimalFormat("###0.000");

        System.out.println("\nfor " + completedJobs + " jobs the service node statistics are:\n");

        System.out.println("  avg interarrivals .. =   " + f.format(lastArrive / completedJobs));
        System.out.println("  avg wait ........... =   " + f.format(area[0].node / completedJobs));
        System.out.println("  avg # in node ...... =   " + f.format(area[0].node / lastDeparture));

        System.out.println("");
    }

    @Override
    public double getAvgInterarrival(int i) {
        return 0;
    }

    @Override
    public double getAvgWait(int i) {
        return 0;
    }

    @Override
    public double getAvgDelay(int i) {
        return 0;
    }

    @Override
    public double getAvgNode(int i) {
        return 0;
    }

    @Override
    public double getAvgQueue(int i) {
        return 0;
    }

    @Override
    public double getUtilization(int i) {
        return 0;
    }
}
