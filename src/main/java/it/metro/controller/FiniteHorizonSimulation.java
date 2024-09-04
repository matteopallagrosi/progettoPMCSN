package it.metro.controller;

import it.metro.utils.Rngs;
import it.metro.utils.Rvgs;

public class FiniteHorizonSimulation {

    private final int NUM_REPLICATIONS = 128;
    Simulation[] simulations = new Simulation[NUM_REPLICATIONS];

    public static void main(String[] args) {
        FiniteHorizonSimulation finiteHorizonSImulation = new FiniteHorizonSimulation();
        finiteHorizonSImulation.run();
    }

    private void run() {

        //Per garantire che le repliche non abbiano sovrapposizioni tra di loro per nessuno degli stream,
        //lo stato finale di ogni stream diventa lo stato iniziale di quello stesso stream nella replica successiva
        //questo è ottenuto realizzando la PlantSeeds una volta prima del ciclo che realizza le replica
        Rngs r = new Rngs();
        r.plantSeeds(123456789);
        Rvgs v = new Rvgs(r);

        //configurazione dei centri (numero server per ogni centro) nelle diverse fasce orarie
        //ogni riga corrisponde a una fascia oraria, l'i-esima colonna di una riga corrisponde al numero di server in quella fascia per l'i-esimo centro
        int[][] configCenters = new int[][] {{3,3,3,3,3}, {4,4,4,2,2}, {3,4,4,2,4}, {5,5,3,4,2}, {4,4,2,2,2}, {4,4,4,2,2}, {4,4,4,2,2}};
        //int[][] configCenters = new int[][] {{4,4,4,4,4}, {4,4,4,4,4}, {4,4,4,4,4}, {4,4,4,4,4}, {4,4,4,4,4}, {4,4,4,4,4}, {4,4,4,4,4}};

        //tassi di arrivo di ognuna delle fasce orarie
        double[] slotRates = new double[] {0.045, 0.303, 0.076, 0.606, 0.05, 0.273, 0.076};

        //esegue NUM_REPLICATIONS repliche, ciascuna delle quali simula le 18 ore di operatività della metropolitana
        for (int i = 0; i < NUM_REPLICATIONS; i++) {
            System.out.println("Replica n. " + (i+1));
            Simulation simulation = new Simulation();
            //gli stati iniziali di ciascuno stream saranno gli stati finali della replica precedente
            simulation.initSeed(r, v);
            //L'arrivo degli utenti viene interrotto dopo 18 ore = 648000 (alle 23.30 orario di chiusura della metro, aperta alle 5.30)
            simulation.setStop(64800);
            simulation.runFiniteHorizonSimulation(configCenters, slotRates);
            simulations[i] = simulation;
        }
        System.out.println("FINITO!");
    }
}
