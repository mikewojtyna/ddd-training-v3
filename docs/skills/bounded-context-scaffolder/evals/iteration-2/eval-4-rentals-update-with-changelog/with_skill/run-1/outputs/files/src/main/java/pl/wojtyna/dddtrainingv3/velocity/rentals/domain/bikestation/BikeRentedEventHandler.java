package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

public class BikeRentedEventHandler {

    public void handle(BikeRentalEvent event) {
        switch (event) {
            case BikeRentalEvent.BikePlacedInStation bikePlacedInStation -> {
            }
            case BikeRentalEvent.BikeRented bikeRented -> {
            }
            case BikeRentalEvent.BikeReturned bikeReturned -> {
            }
        }
    }
}
