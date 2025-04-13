package com.yassine.portfolio_tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yassine.portfolio_tracker.model.Portfolio;
import com.yassine.portfolio_tracker.model.Stock;
import com.yassine.portfolio_tracker.dto.StockQuote;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.yassine.portfolio_tracker.repository.StockRepository;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;


@Service
public class StockService {

    private final StockRepository stockRepository;
    @Value("${twelvedata.api.key}")
    private String apiKey;
    private final RestTemplate restTemplate;

    // private final List<String> topSymbols = List.of("AAPL", "MSFT", "AMZN", "GOOGL", "NVDA", "META", "TSLA");
    private final List<String> topSymbols = List.of("AAPL", "MSFT");


    public StockService(RestTemplate restTemplate, StockRepository stockRepository) {
        this.restTemplate = restTemplate;
        this.stockRepository = stockRepository;
    }


    @Getter
    private StockQuote topPerformer;


    public List<StockQuote> getTopStockPrices() {
        List<StockQuote> results = new ArrayList<>();

        for (String symbol : topSymbols) {
            String url = "https://api.twelvedata.com/quote?symbol=" + symbol + "&apikey=" + apiKey;

            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
                ObjectMapper mapper = new ObjectMapper();
                System.out.println("API Response for " + symbol + ": " + response.getBody());

                JsonNode root = mapper.readTree(response.getBody());

                StockQuote quote = new StockQuote();
                quote.setSymbol(root.path("symbol").asText());
                quote.setName(root.path("name").asText());
                quote.setClose(root.path("close").asDouble());
                quote.setPreviousClose(root.path("previous_close").asDouble());
                quote.setChange(root.path("change").asDouble());
                quote.setPercentChange(root.path("percent_change").asDouble());
                quote.setVolume(root.path("volume").asInt());

                results.add(quote);

                // Check if this quote is the top performer
                if (topPerformer == null || quote.getPercentChange() > topPerformer.getPercentChange()) {
                    topPerformer = quote;
                }

            } catch (Exception e) {
                System.out.println("Failed to fetch quote for " + symbol + ": " + e.getMessage());
            }
        }

        // Optional: Log or store the top performer
        if (topPerformer != null) {
            System.out.println("Top performer: " + topPerformer.getSymbol() + " (" + topPerformer.getPercentChange() + "%)");
        }

        return results;
    }


    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }


    public void deleteStock(Long id) {
        stockRepository.deleteById(id);
    }
}