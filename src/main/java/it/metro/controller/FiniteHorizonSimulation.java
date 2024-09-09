package it.metro.controller;

import it.metro.centers.Center;
import it.metro.centers.MslsCenter;
import it.metro.centers.MsmqCenter;
import it.metro.centers.MssqCenter;
import it.metro.utils.*;

import java.awt.image.SampleModel;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
        int[][] configCenters = new int[][] {{3,1,1,1,1}, {20,3,3,2,1}, {5,1,1,1,1}, {42,6,5,7,1}, {4,1,1,1,1}, {19,3,2,2,1}, {5,1,1,1,1}};;
        //int[][] configCenters = new int[][] {{4,4,4,4,4}, {4,4,4,4,4}, {4,4,4,4,4}, {4,4,4,4,4}, {4,4,4,4,4}, {4,4,4,4,4}, {4,4,4,4,4}};

        //tassi di arrivo di ognuna delle fasce orarie
        double[] slotRates = new double[] {0.045, 0.303, 0.076, 0.606, 0.05, 0.273, 0.076};

        //esegue NUM_REPLICATIONS repliche, ciascuna delle quali simula le 18 ore di operatività della metropolitana
        for (int i = 0; i < NUM_REPLICATIONS; i++) {
            System.out.println("Replica n. " + (i+1));
            Simulation simulation = new Simulation();
            simulation.rvms = new Rvms();
            //gli stati iniziali di ciascuno stream saranno gli stati finali della replica precedente
            simulation.initSeed(r, v);
            //L'arrivo degli utenti viene interrotto dopo 18 ore = 648000 s (alle 23.30 orario di chiusura della metro, aperta alle 5.30)
            simulation.setStop(64800);
            if (i == 1) {
                System.out.println("ciao");
            }
            simulation.runFiniteHorizonSimulation(configCenters, slotRates);
            simulations[i] = simulation;
        }

        //costruisco la matriche che contiene le statistiche mediate su tutte le repliche
        //la matrice ha una riga per ogni centro, e una colonna per ogni slot di campionamento
        //contiene per ogni centro e slot di campionamento la media su tutte le repliche di ogni statistica di quel centro
        Statistics[][] matrix = new Statistics[6][54];
        //per ogni centro (+ overall del sistema)
        for (int i = 0; i < 6; i++) {
            //per ogni slot di campionamento
            for (int j = 0; j < 54; j++) {
                Statistics currentStat = new Statistics(1);
                //per ogni replica
                for (int k = 0; k < NUM_REPLICATIONS; k++) {
                    currentStat.avgWait[0] += simulations[k].matrix[i][j].avgWait[0];
                    currentStat.avgNode[0] += simulations[k].matrix[i][j].avgNode[0];
                    if (i != 5) {
                        currentStat.avgDelay[0] += simulations[k].matrix[i][j].avgDelay[0];
                        currentStat.avgInterarrivals[0] += simulations[k].matrix[i][j].avgInterarrivals[0];
                        currentStat.avgQueue[0] += simulations[k].matrix[i][j].avgQueue[0];
                        currentStat.utilization[0] += simulations[k].matrix[i][j].utilization[0];
                    }
                    //il centro 4 (quindi i == 3) dei controllori ha anche la probabilità di loss
                    if (i == 3) {
                        currentStat.lossProbability += simulations[k].matrix[i][j].lossProbability;
                    }

                }
                //media su tutte le repliche
                currentStat.avgWait[0] = currentStat.avgWait[0] / NUM_REPLICATIONS;
                currentStat.avgNode[0] =  currentStat.avgNode[0] / NUM_REPLICATIONS;
                if (i != 5) {
                    currentStat.avgDelay[0] = currentStat.avgDelay[0] / NUM_REPLICATIONS;
                    currentStat.avgInterarrivals[0] = currentStat.avgInterarrivals[0] / NUM_REPLICATIONS;
                    currentStat.avgQueue[0] = currentStat.avgQueue[0] / NUM_REPLICATIONS;
                    currentStat.utilization[0] = currentStat.utilization[0] / NUM_REPLICATIONS;
                }
                if (i == 3) {
                    currentStat.lossProbability = currentStat.lossProbability / NUM_REPLICATIONS;
                }
                matrix[i][j] = currentStat;
            }
        }

        generateSamplingEstimate(matrix, 54);
    }

    private void generateSamplingEstimate(Statistics[][] matrix, int numSampling) {
        String[] centerNames = {"Electronic Ticket Center", "Ticket center", "Turnstiles center", "Ticket Inspectors Center", "Elevators", "System"};
        for (int i = 0; i < 5; i++) {
            double[] avgInterrarivals = new double[numSampling];
            double[] avgWait = new double[numSampling];
            double[] avgDelay = new double[numSampling];
            double[] avgService = new double[numSampling];
            double[] avgNode = new double[numSampling];
            double[] avgQueue = new double[numSampling];
            double[] utilization = new double[numSampling];
            double[] lossProbabilities = new double[numSampling];
            for (int j = 0; j < numSampling; j++) {
                Statistics currentStatistics = matrix[i][j];
                avgInterrarivals[j] = currentStatistics.avgInterarrivals[0];
                avgWait[j] = currentStatistics.avgWait[0];
                avgDelay[j] = currentStatistics.avgDelay[0];
                avgService[j] = currentStatistics.avgService[0];
                avgNode[j] = currentStatistics.avgNode[0];
                avgQueue[j] = currentStatistics.avgQueue[0];
                utilization[j] = currentStatistics.utilization[0];
                if (i == 3) {
                    lossProbabilities[j] = currentStatistics.lossProbability;
                }
            }
            //stampo gli intervalli di confidenza per le diverse statistiche di questo centro
            System.out.println("For " + centerNames[i] + ":");
            System.out.println("interrarivals:");
            Estimate.estimate(avgInterrarivals);
            generateDatFile(avgInterrarivals, centerNames[i], "avgInterrarivals");
            System.out.println("wait:");
            Estimate.estimate(avgWait);
            generateDatFile(avgWait, centerNames[i], "avgWait");
            System.out.println("delay:");
            Estimate.estimate(avgDelay);
            generateDatFile(avgDelay, centerNames[i], "avgDelay");
            System.out.println("jobs in node:");
            Estimate.estimate(avgNode);
            generateDatFile(avgNode, centerNames[i], "avgNode");
            System.out.println("jobs in queue:");
            Estimate.estimate(avgQueue);
            generateDatFile(avgQueue, centerNames[i], "avgQueue");
            System.out.println("utilization:");
            Estimate.estimate(utilization);
            generateDatFile(utilization, centerNames[i], "utilization");
            if (i == 3) {
                System.out.println("loss Probability: ");
                Estimate.estimate(lossProbabilities);
                generateDatFile(lossProbabilities, centerNames[i], "lossProbability");
            }
        }

        //genero l'intervallo di confidenza per le statistiche globali del sistema
        double[] avgWait = new double[numSampling];
        double[] avgNode = new double[numSampling];
        for (int i = 0; i < numSampling; i++) {
            Statistics currentStatistics = matrix[5][i];
            avgWait[i] = currentStatistics.avgWait[0];
            avgNode[i] = currentStatistics.avgNode[0];
        }
        System.out.println("For overall system:");
        System.out.println("wait:");
        Estimate.estimate(avgWait);
        generateDatFile(avgWait, "system", "avgWait");
        System.out.println("jobs in node:");
        Estimate.estimate(avgNode);
        generateDatFile(avgNode, "system", "avgNode");
    }

    private static void generateDatFile(double[] sample, String centerName, String statName) {
        // Scrivere i valori double nel file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(centerName + "_" + statName + ".dat"))) {
            for (double x : sample) {
                // Scrive il double convertito in stringa e va a capo
                writer.write(Double.toString(x));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
