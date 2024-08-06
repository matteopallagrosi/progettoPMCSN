package it.metro.centers;

//rappresenta il centro con gli ascensori per raggiungere la banchina
public class ElevatorsCenter extends Center {

    public ElevatorsCenter(int numServer) {
        super(5, numServer);
    }

    @Override
    public Center getNextCenter() {
        return null;
    }

    @Override
    public void processArrival() {

    }

    @Override
    public void processDeparture() {

    }
}
