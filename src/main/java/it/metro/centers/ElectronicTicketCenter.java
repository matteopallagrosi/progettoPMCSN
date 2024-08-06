package it.metro.centers;

//rappresenta il centro delle casse automatiche
public class ElectronicTicketCenter extends Center {

    public ElectronicTicketCenter(int numServer) {
        super(1, numServer);
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
