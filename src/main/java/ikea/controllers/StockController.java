package ikea.controllers;

import ikea.services.EmailSender;
import ikea.services.StockCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/1.0/stock")
public class StockController {

    @Autowired
    private StockCheck stockCheck;

    @Autowired
    private EmailSender emailSender;

    @GetMapping
    public ResponseEntity<String> checkIfStockExists(@RequestParam("id") final String id,
                                                     @RequestParam("email") final String email) {
        try {
            final boolean availableForHomeDelivery = stockCheck.checkAvailableForHomeDelivery(id);
            if(availableForHomeDelivery) {
                emailSender.sendSimpleMessage(email, "Stock Available!", "Stock is available for " + id + ", go check!");
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch(final Exception exception) {
            exception.printStackTrace();

            emailSender.sendSimpleMessage(email, "Ikea Stock Check FAIL!", exception.getMessage());
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }
}
