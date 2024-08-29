package com.calllifo.http;

import java.io.FileInputStream;
import java.util.Properties;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class PriceHandler {
	private static String KEY;
	static String stockIndex = "";
	double lastThreePrices[] = { 0.0, 0.0, 0.0 };
	int cnt_Index = 0;

	public void tradeActionAt(double price) {
		System.out.println("Price of the Stock:" + stockIndex + " of Price:" + price);
	}

	public void tradeActionAtBkp(double price) {

		lastThreePrices[cnt_Index] = price;
		cnt_Index++;
		cnt_Index = cnt_Index % 3;

		int index_1 = (cnt_Index - 1) < 0 ? ((cnt_Index - 1) + 3) % 3 : (cnt_Index - 1) % 3;
		int index_2 = (cnt_Index - 2) < 0 ? ((cnt_Index - 2) + 3) % 3 : (cnt_Index - 2) % 3;
		int index_3 = (cnt_Index - 3) < 0 ? ((cnt_Index - 3) + 3) % 3 : (cnt_Index - 3) % 3;

		if ((Math.abs(lastThreePrices[index_1] - lastThreePrices[index_2]) <= 1)
				&& (Math.abs(lastThreePrices[index_3] - lastThreePrices[index_2]) <= 1)) {
			System.out.println("###########MARKET IS DULL NO ACTION REQUIRED#################");
			return;
		}

		if (((lastThreePrices[index_1] > lastThreePrices[index_2])
				&& (lastThreePrices[index_2] > lastThreePrices[index_3]))
				&& ((lastThreePrices[index_1] < lastThreePrices[index_2])
						&& (lastThreePrices[index_2] > lastThreePrices[index_3])
						&& lastThreePrices[index_1] > lastThreePrices[index_3])) {
			System.out.print("#############STOP STOCK#############");
			System.out.println(
					lastThreePrices[index_1] + ">" + lastThreePrices[index_2] + ">" + lastThreePrices[index_3]);
			System.out.println("***************************************************");
			try {
				cancelOrder(stockIndex);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}

		if ((lastThreePrices[index_1] < lastThreePrices[index_2])
				&& (lastThreePrices[index_2] > lastThreePrices[index_3])
				&& lastThreePrices[index_1] < lastThreePrices[index_3]) {
			System.out.print("#############HOLD STOCK#############");
			System.out.println(
					lastThreePrices[index_1] + "<" + lastThreePrices[index_2] + ">" + lastThreePrices[index_3]);
			System.out.println("***************************************************");
			return;
		}

		if (((lastThreePrices[index_1] > lastThreePrices[index_2])
				&& (lastThreePrices[index_2] < lastThreePrices[index_3]))
				|| ((lastThreePrices[index_1] < lastThreePrices[index_2])
						&& (lastThreePrices[index_2] < lastThreePrices[index_3]))) {
			System.out.print("#############START STOCK#############");
			System.out.println(
					lastThreePrices[index_1] + ">" + lastThreePrices[index_2] + "<" + lastThreePrices[index_3]);
			System.out.println("***************************************************");
			try {
				placeOrder(stockIndex);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public PriceHandler(String key, String stockIndex) {
		KEY = key;
		this.stockIndex = stockIndex;
	}

	public String toString() {
		return lastThreePrices[0] + "#" + lastThreePrices[1] + "#" + lastThreePrices[2];
	}

	public static void placeOrder(String stockIndex) throws Exception {

		HttpResponse<String> response = null;
		Unirest.setTimeouts(0, 0);

		try (FileInputStream input = new FileInputStream(
				"C:\\workspace\\isaiah_60_5\\src\\main\\resources\\order.properties")) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			String jsonStr = TradeMontor.placeorderMap.get(stockIndex);//prop.getProperty(stockIndex);
			System.out.println(jsonStr);
			response = Unirest
					.post("https://ant.aliceblueonline.com/rest/AliceBlueAPIService/api/placeOrder/executePlaceOrder")
					.header("Authorization", KEY).header("Content-Type", "application/json").body(jsonStr).asString();
			System.out.println(response.getBody());

		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void cancelOrder(String stockIndex) throws Exception {

		HttpResponse<String> response = null;
		Unirest.setTimeouts(0, 0);

		try (FileInputStream input = new FileInputStream(
				"C:\\workspace\\isaiah_60_5\\src\\main\\resources\\cancelorder.properties")) {

			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			String jsonStr = prop.getProperty(stockIndex);
			System.out.println(jsonStr);
			response = Unirest.post(
					"https://ant.aliceblueonline.com/rest/AliceBlueAPIService/api/placeOrder/cancelOrder")
					.header("Authorization", KEY).header("Content-Type", "application/json").body(jsonStr).asString();
			System.out.println(response.getBody());

		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
