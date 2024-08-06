package it.metro.centers;

//rappresenta il centro delle casse fisiche (ossia con persona a servire)
public class TicketCenter extends Center {

    public TicketCenter(int numServer) {
        super(2, numServer);
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
