package com.yassine.portfolio_tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yassine.portfolio_tracker.dto.StockQuote;
import com.yassine.portfolio_tracker.model.StockCache;
import com.yassine.portfolio_tracker.repository.StockCacheRepository;
import com.yassine.portfolio_tracker.repository.StockRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class StockService {

    private final StockRepository stockRepository;
    private final StockCacheRepository stockCacheRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${twelvedata.api.key}")
    private String apiKey;

    private final List<String> topSymbols = List.of("AAPL", "MSFT", "AMZN", "GOOGL", "NVDA", "META", "TSLA");

    // Last fetched quotes — populated by getTopStockPrices(), read by getTopPerformer()
    private final AtomicReference<List<StockQuote>> lastQuotes = new AtomicReference<>(List.of());

    public StockService(RestTemplate restTemplate, StockRepository stockRepository, StockCacheRepository stockCacheRepository) {
        this.restTemplate = restTemplate;
        this.stockRepository = stockRepository;
        this.stockCacheRepository = stockCacheRepository;
    }

    public List<StockQuote> getTopStockPrices() {
        List<StockQuote> results = new ArrayList<>();

        for (String symbol : topSymbols) {
            StockQuote quote = fetchFromApi(symbol).orElseGet(() -> fetchFromCache(symbol).orElse(null));

            if (quote != null && quote.getSymbol() != null && quote.getClose() != 0) {
                results.add(quote);
            }
        }

        lastQuotes.set(results);
        return results;
    }

    public Optional<StockQuote> getTopPerformer() {
        List<StockQuote> quotes = lastQuotes.get();
        if (quotes.isEmpty()) {
            quotes = getTopStockPrices();
        }
        return quotes.stream().max(Comparator.comparingDouble(StockQuote::getPercentChange));
    }

    public Optional<StockQuote> getQuote(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return Optional.empty();
        }

        String normalizedSymbol = symbol.trim().toUpperCase();
        return fetchFromApi(normalizedSymbol).or(() -> fetchFromCache(normalizedSymbol));
    }

    private Optional<StockQuote> fetchFromApi(String symbol) {
        String url = "https://api.twelvedata.com/quote?symbol=" + symbol + "&apikey=" + apiKey;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            // TwelveData returns {"code":4xx,"message":"..."} on errors
            if (root.has("code") || root.has("message")) {
                log.warn("TwelveData API error for {}: {}", symbol, root.path("message").asText());
                return Optional.empty();
            }

            StockQuote quote = new StockQuote();
            quote.setSymbol(root.path("symbol").asText());
            quote.setName(root.path("name").asText());
            quote.setClose(root.path("close").asDouble());
            quote.setPreviousClose(root.path("previous_close").asDouble());
            quote.setChange(root.path("change").asDouble());
            quote.setPercentChange(root.path("percent_change").asDouble());
            quote.setVolume(root.path("volume").asInt());

            if (quote.getClose() != 0) {
                stockCacheRepository.save(convertToStockCache(quote));
            }
            return Optional.of(quote);
        } catch (Exception e) {
            log.error("API call failed for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<StockQuote> fetchFromCache(String symbol) {
        return stockCacheRepository.findBySymbol(symbol).map(this::convertToStockQuote);
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

    public void deleteStock(Long id) {
        stockRepository.deleteById(id);
    }
}
