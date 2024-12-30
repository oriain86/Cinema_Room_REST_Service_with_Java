package cinema;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;



@RestController
public class CinemaController {
    private final int rows = 9;
    private final int columns = 9;
    private final List<Seat> seats = new ArrayList<>();
    private final Map<String, Seat> purchasedTickets = new HashMap<>();
    private final String SUPER_SECRET_PASSWORD = "super_secret";

    public CinemaController() {
        // Initialize all seats
        for (int row = 1; row <= rows; row++) {
            for (int column = 1; column <= columns; column++) {
                seats.add(new Seat(row, column));
            }
        }
    }

    @GetMapping("/seats")
    public Map<String, Object> getSeats() {
        List<Map<String, Object>> availableSeats = new ArrayList<>();

        for (Seat seat : seats) {
            if (seat.isAvailable()) {
                Map<String, Object> seatInfo = new HashMap<>();
                seatInfo.put("row", seat.getRow());
                seatInfo.put("column", seat.getColumn());
                seatInfo.put("price", seat.getPrice());
                availableSeats.add(seatInfo);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("rows", rows);
        response.put("columns", columns);
        response.put("seats", availableSeats);

        return response;
    }

    @PostMapping("/purchase")
    public ResponseEntity<Object> purchaseTicket(@RequestBody Map<String, Integer> request) {
        int requestedRow = request.get("row");
        int requestedColumn = request.get("column");

        if (requestedRow < 1 || requestedRow > rows ||
                requestedColumn < 1 || requestedColumn > columns) {
            return new ResponseEntity<>(Map.of("error",
                    "The number of a row or a column is out of bounds!"),
                    HttpStatus.BAD_REQUEST);
        }

        for (Seat seat : seats) {
            if (seat.getRow() == requestedRow && seat.getColumn() == requestedColumn) {
                if (!seat.isAvailable()) {
                    return new ResponseEntity<>(Map.of("error",
                            "The ticket has been already purchased!"),
                            HttpStatus.BAD_REQUEST);
                }

                seat.setAvailable(false);
                String token = UUID.randomUUID().toString();
                purchasedTickets.put(token, seat);

                return new ResponseEntity<>(Map.of(
                        "token", token,
                        "ticket", Map.of(
                                "row", seat.getRow(),
                                "column", seat.getColumn(),
                                "price", seat.getPrice()
                        )
                ), HttpStatus.OK);
            }
        }

        return new ResponseEntity<>(Map.of("error", "Unexpected error occurred!"),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PostMapping("/return")
    public ResponseEntity<Object> returnTicket(@RequestBody Map<String, String> request) {
        String token = request.get("token");

        if (!purchasedTickets.containsKey(token)) {
            return new ResponseEntity<>(Map.of("error", "Wrong token!"),
                    HttpStatus.BAD_REQUEST);
        }

        Seat seat = purchasedTickets.remove(token);
        seat.setAvailable(true);

        return new ResponseEntity<>(Map.of(
                "ticket", Map.of(
                        "row", seat.getRow(),
                        "column", seat.getColumn(),
                        "price", seat.getPrice()
                )
        ), HttpStatus.OK);
    }

    @GetMapping("/stats")
    public ResponseEntity<Object> getStatistics(@RequestParam(required = false) String password) {
        if (password == null || !password.equals(SUPER_SECRET_PASSWORD)) {
            return new ResponseEntity<>(Map.of("error", "The password is wrong!"),
                    HttpStatus.UNAUTHORIZED);
        }

        int income = purchasedTickets.values().stream().mapToInt(Seat::getPrice).sum();
        int available = (int) seats.stream().filter(Seat::isAvailable).count();
        int purchased = purchasedTickets.size();

        return new ResponseEntity<>(Map.of(
                "income", income,
                "available", available,
                "purchased", purchased
        ), HttpStatus.OK);
    }
}
