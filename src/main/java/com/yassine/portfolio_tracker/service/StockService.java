package com.yassine.portfolio_tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yassine.portfolio_tracker.model.Portfolio;
import com.yassine.portfolio_tracker.model.Stock;
import com.yassine.portfolio_tracker.dto.StockQuote;
import com.yassine.portfolio_tracker.model.StockCache;
import com.yassine.portfolio_tracker.repository.StockCacheRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.yassine.portfolio_tracker.repository.StockRepository;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class StockService {

    private final StockRepository stockRepository;
    private final StockCacheRepository stockCacheRepository;

    @Value("${twelvedata.api.key}")
    private String apiKey;
    private final RestTemplate restTemplate;

    private final List<String> topSymbols = List.of("AAPL", "MSFT", "AMZN", "GOOGL", "NVDA", "META", "TSLA");
    // private final List<String> topSymbols = List.of("AAPL", "MSFT" ,"AMZN","GOOGL");


    public StockService(RestTemplate restTemplate, StockRepository stockRepository, StockCacheRepository stockCacheRepository) {
        this.restTemplate = restTemplate;
        this.stockRepository = stockRepository;
        this.stockCacheRepository = stockCacheRepository;
    }


    @Getter
    private StockQuote topPerformer;


    public List<StockQuote> getTopStockPrices() {
        List<StockQuote> results = new ArrayList<>();

        for (String symbol : topSymbols) {
            StockQuote quote = null;

            // Try fetching data from API first
            String url = "https://api.twelvedata.com/quote?symbol=" + symbol + "&apikey=" + apiKey;
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
                ObjectMapper mapper = new ObjectMapper();
                System.out.println("API Response for " + symbol + ": " + response.getBody());

                JsonNode root = mapper.readTree(response.getBody());
                if(root != null) {
                    quote = new StockQuote();
                    quote.setSymbol(root.path("symbol").asText());
                    quote.setName(root.path("name").asText());
                    quote.setClose(root.path("close").asDouble());
                    quote.setPreviousClose(root.path("previous_close").asDouble());
                    quote.setChange(root.path("change").asDouble());
                    quote.setPercentChange(root.path("percent_change").asDouble());
                    quote.setVolume(root.path("volume").asInt());
                    if (quote.getSymbol() != null && quote.getClose() != 0) {
                        StockCache stockCache = convertToStockCache(quote);
                        System.out.println(stockCache);
                        stockCacheRepository.save(stockCache);
                    }
                }



            } catch (Exception e) {
                System.out.println("API call failed for " + symbol + ": " + e.getMessage());
                Optional<StockCache> cachedStock = stockCacheRepository.findBySymbol(symbol);
                if (cachedStock.isPresent()) {
                    StockCache stockCache = cachedStock.get();
                    quote = convertToStockQuote(stockCache);
                }
            }

            // Ensure that only non-null and valid StockQuote is added to results
            if (quote != null && quote.getSymbol() != null && quote.getClose() != 0) {
                results.add(quote);

                // Check if this quote is the top performer
                if (topPerformer == null || quote.getPercentChange() > topPerformer.getPercentChange()) {
                    topPerformer = quote;
                }
            }
        }

        // Optional: Log or store the top performer
        if (topPerformer != null) {
            System.out.println("Top performer: " + topPerformer.getSymbol() + " (" + topPerformer.getPercentChange() + "%)");
        }

        return results;
    }

    private StockQuote convertToStockQuote(StockCache stockCache) {
        StockQuote quote = new StockQuote();
        quote.setSymbol(stockCache.getSymbol());
        quote.setName(stockCache.getName());
        quote.setClose(stockCache.getClose());
        quote.setPreviousClose(stockCache.getPreviousClose());
        quote.setChange(stockCache.getChange());
        quote.setPercentChange(stockCache.getPercentChange());
        quote.setVolume(stockCache.getVolume());
        return quote;
    }

    private StockCache convertToStockCache(StockQuote stockQuote) {
        StockCache stockCache = new StockCache();
        stockCache.setSymbol(stockQuote.getSymbol());
        stockCache.setName(stockQuote.getName());
        stockCache.setClose(stockQuote.getClose());
        stockCache.setPreviousClose(stockQuote.getPreviousClose());
        stockCache.setChange(stockQuote.getChange());
        stockCache.setPercentChange(stockQuote.getPercentChange());
        stockCache.setVolume(stockQuote.getVolume());
        return stockCache;
    }

    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }


    public void deleteStock(Long id) {
        stockRepository.deleteById(id);
    }
}