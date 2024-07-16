import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

class TradeHistory {
    long id;
    String date;
    String contract;
    double price;
    int quantity;

    // TradeHistory constructor
    public TradeHistory(long id, String date, String contract, double price, int quantity) {
        this.id = id;
        this.date = date;
        this.contract = contract;
        this.price = price;
        this.quantity = quantity;
    }

    public long getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getContract() {
        return contract;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }
    // equals metodu: İki TradeHistory nesnelerinde eşitlik arar
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeHistory that = (TradeHistory) o;
        return Double.compare(that.price, price) == 0 && quantity == that.quantity && contract.equals(that.contract);
    }
    // hashCode metodu: TradeHistory nesneleri için hash kodu üretir
    @Override
    public int hashCode() {
        return Objects.hash(contract, price, quantity);
    }
}

public class Main {
    public static void main(String[] args) {
        String startDate = "2024-07-13";
        String endDate = "2024-07-13";
        String apiUrl = "https://seffaflik.epias.com.tr/transparency/service/market/intra-day-trade-history?endDate=" + endDate + "&startDate=" + startDate;

        try {
            // HTTP GET isteği
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            conn.disconnect();

            // JSON verisini işleme
            JSONObject jsonResponse = new JSONObject(content.toString());
            JSONArray tradeHistoryList = jsonResponse.getJSONObject("body").getJSONArray("intraDayTradeHistoryList");

            // Trade verileri için liste
            List<TradeHistory> trades = new ArrayList<>();
            for (int i = 0; i < tradeHistoryList.length(); i++) {
                JSONObject trade = tradeHistoryList.getJSONObject(i);
                long id = trade.getLong("id");
                String date = trade.getString("date");
                String contract = trade.getString("conract");
                double price = trade.getDouble("price");
                int quantity = trade.getInt("quantity");

                trades.add(new TradeHistory(id, date, contract, price, quantity));
            }

            // Contract değerine göre gruplama ve hesaplamalar
            Map<String, Double> totalTradeAmountMap = new HashMap<>();
            Map<String, Double> totalTradeValueMap = new HashMap<>();
            Set<TradeHistory> uniqueTrades = new HashSet<>();

            for (TradeHistory trade : trades) {
                String contract = trade.getContract();
                double price = trade.getPrice();
                int quantity = trade.getQuantity();

                double tradeValue = (price * quantity) / 10.0;
                double tradeAmount = quantity / 10.0;

                totalTradeValueMap.put(contract, totalTradeValueMap.getOrDefault(contract, 0.0) + tradeValue);
                totalTradeAmountMap.put(contract, totalTradeAmountMap.getOrDefault(contract, 0.0) + tradeAmount);

                uniqueTrades.add(trade);
            }

            // Excel dosyasına yazma
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Trade Data");

            // Başlık satırı oluşturma
            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"ID", "Date", "Contract", "Price", "Quantity", "Toplam İşlem Tutarı", "Toplam İşlem Miktarı", "Ağırlıklı Ortalama Fiyat"};
            // Başlıkları hücrelere yazma
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Benzersiz trade verilerini Excel dosyasına yazma
            for (TradeHistory trade : uniqueTrades) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(trade.getId());
                row.createCell(1).setCellValue(trade.getDate());
                row.createCell(2).setCellValue(trade.getContract());
                row.createCell(3).setCellValue(trade.getPrice());
                row.createCell(4).setCellValue(trade.getQuantity());

                // Toplam işlem tutarı, miktarı ve ağırlıklı ortalama fiyat hesaplama
                double totalTradeAmount = totalTradeAmountMap.get(trade.getContract());
                double totalTradeValue = totalTradeValueMap.get(trade.getContract());
                double weightedAveragePrice = totalTradeValue / totalTradeAmount;

                row.createCell(5).setCellValue(totalTradeAmount);
                row.createCell(6).setCellValue(totalTradeValue);
                row.createCell(7).setCellValue(weightedAveragePrice);
            }

            try (FileOutputStream fileOut = new FileOutputStream("data.xlsx")) {
                workbook.write(fileOut);
            }

            workbook.close();
            System.out.println("Excele yazıldı");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
