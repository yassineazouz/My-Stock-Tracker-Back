package com.yassine.portfolio_tracker.service;

import com.yassine.portfolio_tracker.dto.BuyStockRequest;
import com.yassine.portfolio_tracker.dto.StockQuote;
import com.yassine.portfolio_tracker.model.Portfolio;
import com.yassine.portfolio_tracker.model.Stock;
import com.yassine.portfolio_tracker.repository.PortfolioRepository;
import com.yassine.portfolio_tracker.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock StockRepository stockRepository;
    @Mock PortfolioRepository portfolioRepository;
    @Mock StockService stockService;
    @InjectMocks PortfolioService portfolioService;

    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolio = Portfolio.builder()
                .id(1L)
                .owner("yassine")
                .walletValue(10_000.0)
                .build();
    }

    @Test
    void buyStock_newPosition_deductsWalletAndSaves() {
        BuyStockRequest request = new BuyStockRequest();
        request.setSymbol("AAPL");
        request.setCompanyName("Apple Inc.");
        request.setQuantity(2);

        StockQuote quote = new StockQuote();
        quote.setClose(150.0);

        when(portfolioRepository.findByOwner("yassine")).thenReturn(Optional.of(portfolio));
        when(stockService.getQuote("AAPL")).thenReturn(Optional.of(quote));
        when(stockRepository.findBySymbolAndPortfolioId("AAPL", 1L)).thenReturn(Optional.empty());
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        Stock saved = portfolioService.buyStock(request, "yassine");

        assertThat(saved.getSymbol()).isEqualTo("AAPL");
        assertThat(saved.getQuantity()).isEqualTo(2);
        assertThat(portfolio.getWalletValue()).isEqualTo(9_700.0); // 10000 - 2*150
        verify(portfolioRepository).save(portfolio);
    }

    @Test
    void buyStock_existingPosition_averagesPrice() {
        BuyStockRequest request = new BuyStockRequest();
        request.setSymbol("AAPL");
        request.setCompanyName("Apple Inc.");
        request.setQuantity(2);

        StockQuote quote = new StockQuote();
        quote.setClose(200.0);

        Stock existing = Stock.builder()
                .symbol("AAPL").quantity(2).purchasePrice(100.0).currentPrice(180.0)
                .build();

        when(portfolioRepository.findByOwner("yassine")).thenReturn(Optional.of(portfolio));
        when(stockService.getQuote("AAPL")).thenReturn(Optional.of(quote));
        when(stockRepository.findBySymbolAndPortfolioId("AAPL", 1L)).thenReturn(Optional.of(existing));
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        Stock saved = portfolioService.buyStock(request, "yassine");

        assertThat(saved.getQuantity()).isEqualTo(4);
        assertThat(saved.getPurchasePrice()).isEqualTo(150.0); // avg of 2@100 + 2@200
        assertThat(portfolio.getWalletValue()).isEqualTo(9_600.0); // 10000 - 2*200
    }

    @Test
    void buyStock_insufficientFunds_throws() {
        BuyStockRequest request = new BuyStockRequest();
        request.setSymbol("AAPL");
        request.setCompanyName("Apple Inc.");
        request.setQuantity(100);

        StockQuote quote = new StockQuote();
        quote.setClose(500.0); // 100 * 500 = 50000 > 10000

        when(portfolioRepository.findByOwner("yassine")).thenReturn(Optional.of(portfolio));
        when(stockService.getQuote("AAPL")).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> portfolioService.buyStock(request, "yassine"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not enough funds");
    }

    @Test
    void buyStock_noQuoteAvailable_throws() {
        BuyStockRequest request = new BuyStockRequest();
        request.setSymbol("FAKE");
        request.setCompanyName("Fake Corp");
        request.setQuantity(1);

        when(portfolioRepository.findByOwner("yassine")).thenReturn(Optional.of(portfolio));
        when(stockService.getQuote("FAKE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.buyStock(request, "yassine"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No price data available");
    }

    @Test
    void sellStock_creditsWalletAndDeletes() {
        Stock stock = Stock.builder()
                .id(42L).symbol("MSFT").quantity(3).currentPrice(300.0)
                .portfolio(portfolio)
                .build();

        when(stockRepository.findById(42L)).thenReturn(Optional.of(stock));

        portfolioService.sellStock(42L, "yassine");

        assertThat(portfolio.getWalletValue()).isEqualTo(10_900.0); // 10000 + 3*300
        verify(portfolioRepository).save(portfolio);
        verify(stockRepository).delete(stock);
    }

    @Test
    void sellStock_wrongUser_throws() {
        Portfolio other = Portfolio.builder().id(2L).owner("other").walletValue(5_000.0).build();
        Stock stock = Stock.builder().id(1L).symbol("MSFT").currentPrice(100.0).quantity(1)
                .portfolio(other).build();

        when(stockRepository.findById(1L)).thenReturn(Optional.of(stock));

        assertThatThrownBy(() -> portfolioService.sellStock(1L, "yassine"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to user");
    }
}
