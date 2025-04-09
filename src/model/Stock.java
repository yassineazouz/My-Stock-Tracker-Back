@Entity
public class Stock {
    @Id @GeneratedValue
    private Long id;
    private String symbol;
    private int quantity;
    private double purchasePrice;
    private double currentPrice;
    private LocalDate purchaseDate;
}
