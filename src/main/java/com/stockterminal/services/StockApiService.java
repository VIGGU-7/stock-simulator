package com.stockterminal.services;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class StockApiService {

    public double getLivePrice(String symbol) {
        try {
            //yahoo finance api
            String urlStr = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol + "?interval=1d";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            final int responseCode=conn.getResponseCode();
            if(responseCode==404){
                System.out.println("No stock found with the symbol"+symbol);
            }
            else{
                System.out.println("Could not fetch price for " + symbol + ". Check your internet connection.");
                return -1;
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONObject chart = jsonResponse.getJSONObject("chart");
            JSONArray result = chart.getJSONArray("result");
            JSONObject firstResult = result.getJSONObject(0);
            JSONObject meta = firstResult.getJSONObject("meta");
            return meta.getDouble("regularMarketPrice");

        } catch (Exception e) {
            System.out.println("Error fetching live price: " + e.getMessage());
            return -1;
        }
    }

    public void showTradingViewLink(String symbol) {
        String link = "https://www.tradingview.com/chart/?symbol=" + symbol;
        System.out.println("View live interactive chart here: " + link);
        System.out.println("(CTRL+Click the link if your terminal supports it)");
    }
}
