package it.metro.controller;

import it.metro.centers.*;
import it.metro.events.Event;
import it.metro.utils.Rngs;
import it.metro.utils.Rvgs;
import it.metro.utils.Time;

import java.util.Queue;

//Questa classe realizza una simulazione all'orizzonte infinito del sistema
//permettendo quindi di raccogliere le statistiche stazionarie
public class InfiniteHorizonSimulation {

    public int numBatches;
    public int batchSize;

    //Tra le varie statistiche che la simulazione produce, calcoliamo anche l'autocorrelazione tra i batch con la libreria 'acs'
    //la batch size utilizzata deve avere un'autocorrelazione lag 1 < 0.2
    public InfiniteHorizonSimulation(int numBatches, int batchSize) {
        this.numBatches = numBatches;
        this.batchSize = batchSize;
    }

    public static void main(String[] args) {
        InfiniteHorizonSimulation infiniteHorizonSimulation = new InfiniteHorizonSimulation(128, 10000);
        infiniteHorizonSimulation.run();
    }

    //viene eseguita un unica lunga run, per ogni batch di size batchSize vengono raccolte le statistiche di ogni centro
    //viene poi calcolato l'intervallo di confidenza per ogni statistica utilizzando come campione i valori estratti da ciasun batch
    //quindi la dimensione del campione per la stima di ogni statistica ha dimensione numBatches
    public void run() {
        //Per ogni fascia oraria (cambia solo il lambda) devo runnare la simulazione a orizzonte infinito
        //TODO settare la fascia oraria da simulare
        Simulation simulation = new Simulation();
        simulation.runInfiniteHorizonSimulation(numBatches, batchSize);
    }





}
