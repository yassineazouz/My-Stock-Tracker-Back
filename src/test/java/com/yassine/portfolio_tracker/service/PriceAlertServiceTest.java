package com.yassine.portfolio_tracker.service;

import com.yassine.portfolio_tracker.dto.PriceAlertRequest;
import com.yassine.portfolio_tracker.dto.StockQuote;
import com.yassine.portfolio_tracker.model.AlertDirection;
import com.yassine.portfolio_tracker.model.Portfolio;
import com.yassine.portfolio_tracker.model.PriceAlert;
import com.yassine.portfolio_tracker.repository.PortfolioRepository;
import com.yassine.portfolio_tracker.repository.PriceAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceAlertServiceTest {

    @Mock PriceAlertRepository priceAlertRepository;
    @Mock PortfolioRepository portfolioRepository;
    @Mock StockService stockService;
    @InjectMocks PriceAlertService priceAlertService;

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
    void createAlert_normalizesSymbolAndUsesQuoteData() {
        PriceAlertRequest request = new PriceAlertRequest();
        request.setSymbol(" aapl ");
        request.setTargetPrice(180.0);
        request.setDirection(AlertDirection.ABOVE);

        StockQuote quote = new StockQuote();
        quote.setName("Apple Inc.");
        quote.setClose(175.25);

        when(portfolioRepository.findByOwner("yassine")).thenReturn(Optional.of(portfolio));
        when(stockService.getQuote("AAPL")).thenReturn(Optional.of(quote));
        when(priceAlertRepository.save(any(PriceAlert.class))).thenAnswer(inv -> inv.getArgument(0));
        when(priceAlertRepository.countByPortfolioOwnerAndActiveTrue("yassine")).thenReturn(1L);

        PriceAlert saved = priceAlertService.createAlert("yassine", request);

        assertThat(saved.getSymbol()).isEqualTo("AAPL");
        assertThat(saved.getCompanyName()).isEqualTo("Apple Inc.");
        assertThat(saved.getCurrentPrice()).isEqualTo(175.25);
        assertThat(saved.isActive()).isTrue();
        assertThat(portfolio.getActiveAlerts()).isEqualTo(1);
        verify(portfolioRepository).save(portfolio);
    }

    @Test
    void checkActiveAlerts_marksAboveAlertAsTriggered() {
        PriceAlert alert = PriceAlert.builder()
                .id(10L)
                .portfolio(portfolio)
                .symbol("AAPL")
                .targetPrice(100.0)
                .direction(AlertDirection.ABOVE)
                .active(true)
                .build();

        StockQuote quote = new StockQuote();
        quote.setClose(150.0);

        when(priceAlertRepository.findByActiveTrueOrderByCreatedAtAsc()).thenReturn(List.of(alert));
        when(stockService.getQuote("AAPL")).thenReturn(Optional.of(quote));
        when(priceAlertRepository.countByPortfolioOwnerAndActiveTrue("yassine")).thenReturn(0L);

        List<PriceAlert> triggered = priceAlertService.checkActiveAlerts();

        assertThat(triggered).containsExactly(alert);
        assertThat(alert.isActive()).isFalse();
        assertThat(alert.getTriggeredPrice()).isEqualTo(150.0);
        assertThat(alert.getTriggeredAt()).isNotNull();
        assertThat(alert.getLastCheckedAt()).isNotNull();
        assertThat(portfolio.getActiveAlerts()).isZero();
        verify(priceAlertRepository).saveAll(List.of(alert));
        verify(portfolioRepository).save(portfolio);
    }

    @Test
    void checkActiveAlerts_updatesLastCheckedWhenQuoteMissing() {
        PriceAlert alert = PriceAlert.builder()
                .id(11L)
                .portfolio(portfolio)
                .symbol("MSFT")
                .targetPrice(100.0)
                .direction(AlertDirection.BELOW)
                .active(true)
                .build();

        when(priceAlertRepository.findByActiveTrueOrderByCreatedAtAsc()).thenReturn(List.of(alert));
        when(stockService.getQuote("MSFT")).thenReturn(Optional.empty());

        List<PriceAlert> triggered = priceAlertService.checkActiveAlerts();

        assertThat(triggered).isEmpty();
        assertThat(alert.isActive()).isTrue();
        assertThat(alert.getLastCheckedAt()).isNotNull();
        assertThat(alert.getTriggeredAt()).isNull();
        verify(priceAlertRepository).saveAll(List.of(alert));
        verifyNoInteractions(portfolioRepository);
    }

    @Test
    void deleteAlert_rejectsWrongOwner() {
        Portfolio other = Portfolio.builder().id(2L).owner("other").build();
        PriceAlert alert = PriceAlert.builder().id(20L).portfolio(other).active(true).build();

        when(priceAlertRepository.findById(20L)).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> priceAlertService.deleteAlert("yassine", 20L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to user");

        verify(priceAlertRepository, never()).delete(any());
    }
}
