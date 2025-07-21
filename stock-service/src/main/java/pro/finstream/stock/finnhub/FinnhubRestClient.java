package pro.finstream.stock.finnhub;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "finnhub")
public interface FinnhubRestClient {
    
    @GET
    @Path("/quote")
    FinnhubQuoteDto getQuote(@QueryParam("symbol") String symbol, @QueryParam("token") String token);
}
