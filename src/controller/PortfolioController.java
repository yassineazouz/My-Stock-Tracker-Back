@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {
    @Autowired private StockService stockService;

    @GetMapping
    public List<Stock> getAllStocks() {
        return stockService.getUserStocks(); // à adapter avec utilisateur connecté
    }

    @PostMapping
    public Stock addStock(@RequestBody Stock stock) {
        return stockService.addStock(stock);
    }
}
