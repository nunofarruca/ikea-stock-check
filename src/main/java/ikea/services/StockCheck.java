package ikea.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class StockCheck {

    private static final String API_URL = "https://api.ingka.ikea.com/cia/availabilities/ru/ie?itemNos=%s&expand=StoresList,Restocks";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EmailSender emailSender;

    public boolean checkAvailableForHomeDelivery(final String ikeaId, final String email) throws Exception {
        final String url = String.format(API_URL, ikeaId);

        final ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET, headers(), String.class);
        if(exchange.getStatusCode().is2xxSuccessful()) {
            final String body = exchange.getBody();
            final JsonNode rootNode = new ObjectMapper().readValue(body, JsonNode.class);
            final JsonNode availabilities = rootNode.get("availabilities");

            if(availabilities.size() != 2) {
                throw new RuntimeException("Ikea endpoint returned not expected size");
            }

            for(int i = 0; i < availabilities.size(); i++) {
                final JsonNode jsonNode = availabilities.get(i);
                final JsonNode availableForHomeDelivery = jsonNode.get("availableForHomeDelivery");
                if(availableForHomeDelivery != null && availableForHomeDelivery.asBoolean()) {
                    final String itemNo = jsonNode.get("itemKey").get("itemNo").textValue();
                    final String itemType = jsonNode.get("itemKey").get("itemType").textValue();

                    String ikeaItemUrl = "https://www.ikea.com/ie/en/p/ikea-";
                    if("SPR".equals(itemType)) {
                        ikeaItemUrl += "s";
                    }

                    ikeaItemUrl += itemNo;

                    final String message = "Stock is available for " + ikeaId + ", go check! \n" + ikeaItemUrl;
                    emailSender.sendSimpleMessage(email, "Stock Available!", message);
                    return true;
                }
            }

        } else {
            throw new RuntimeException("Ikea endpoint returned NOT OK");
        }

        return false;
    }

    private HttpEntity headers() {
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:87.0) Gecko/20100101 Firefox/87.0");
        httpHeaders.add("Accept", "application/json;version=2");
        httpHeaders.add("Accept-Language", "en-GB,en;q=0.5");
        httpHeaders.add("Referer", "https://www.ikea.com/");
        httpHeaders.add("X-Client-ID", "b6c117e5-ae61-4ef5-b4cc-e0b1e37f0631");
        httpHeaders.add("Origin", "https://www.ikea.com");
        httpHeaders.add("Connection", "keep-alive");
        httpHeaders.add("Pragma", "no-cache");
        httpHeaders.add("Cache-Control", "no-cache");
        httpHeaders.add("TE", "Trailers");
        return new HttpEntity(null, httpHeaders);
    }
}
