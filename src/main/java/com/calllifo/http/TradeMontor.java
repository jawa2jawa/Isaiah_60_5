
package com.calllifo.http;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.websocket.Session;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

public class TradeMontor {
	private static String projectPath = "C:\\Users\\HP\\Downloads\\isaiah_60_5\\";
	private static String KEY = "";
	protected static String sessionId = "";
	public static HashMap<String, String> placeorderMap = new HashMap<String, String>();
	public static HashMap<String, String> cancelorderMap = new HashMap<String, String>();
	public Session session;
	static CountDownLatch latch = new CountDownLatch(1);
	static ConcurrentHashMap<String, PriceHandler> submittedOrdersMap = new ConcurrentHashMap<String, PriceHandler>();
	static ArrayBlockingQueue<String> priceInfo = new ArrayBlockingQueue<String>(100000);
	Session userSession = null;
	static WebSocket webSocket = null;
	public static Object lock = new Object();
	static String exchangeStr = "NFO";
	static String webSocketPayload = "";

	public TradeMontor() {
		try {

			populateTodaysTrades();

			startThreadPool();
			setup();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void startThreadPool() {

		ExecutorService executor = Executors.newFixedThreadPool(500);
		for (int i = 0; i < 10; i++) {
			Runnable worker = new PriceWatcherThread();
			executor.submit(worker);
		}
	}

	private void getTodaysTrades() {
		String path = projectPath + "isaiah_60_5\\src\\main\\resources\\todaysTrades.txt";
		try {
			PrintWriter out = new PrintWriter(path);

			try {
				String parseLine; /* variable definition */
				/* create objects */
				URL URL = new URL("https://v2api.aliceblueonline.com/restpy/contract_master?exch=NFO");
				BufferedReader br = new BufferedReader(new InputStreamReader(URL.openStream()));

				while ((parseLine = br.readLine()) != null) {
					/* read each line */
					out.println(parseLine);
				}
				br.close();
				out.close();
			} catch (MalformedURLException me) {
				System.out.println(me);

			} catch (IOException ioe) {
				System.out.println(ioe);
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void populateTodaysTrades() {
		// TODO Auto-generated method stub
		String path = projectPath + "isaiah_60_5\\src\\main\\resources\\todaysTrades.txt";
		try {
			getTodaysTrades();
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
			PrintWriter optionsFile = new PrintWriter(
					projectPath + "isaiah_60_5\\src\\main\\resources\\options.properties");
			PrintWriter orderFile = new PrintWriter(
					projectPath + "isaiah_60_5\\src\\main\\resources\\order.properties");
			PrintWriter cancelFile = new PrintWriter(
					projectPath + "isaiah_60_5\\src\\main\\resources\\cancelorder.properties");
			String line = br.readLine();
			System.out.println(line);
			JSONObject jobj = toJsonObject(line);
			JSONArray allTrades = null;

			allTrades = (JSONArray) jobj.get(exchangeStr);
			if (allTrades == null && exchangeStr == "NFO") {
				exchangeStr = "NSE";
				allTrades = (JSONArray) jobj.get(exchangeStr);
			}
			int numberOfTrades = allTrades.size();
			String inputStr = "";

			for (int i = 0; i < numberOfTrades; i++) {
				jobj = (JSONObject) allTrades.get(i);
				inputStr = jobj.get("token") + "="
						+ "[{  \"complexty\": \"regular\",   \"discqty\": \"0\",    \"exch\": \"+exchangeStr+\",    \"pCode\": \"MIS\",    \"prctyp\": \"MKT\",    \"price\": \"0.0\",    \"qty\": "
						+ jobj.get("lot_size") + ",    \"ret\": \"DAY\",    \"symbol_id\":\"" + jobj.get("token")
						+ "\", \"trading_symbol\": \"" + jobj.get("trading_symbol")
						+ "\",  \"transtype\": \"BUY\",   \"trigPrice\": \"0\",   \"orderTag\": \"order1\"       }]";
				// orderFile.write(inputStr);
				placeorderMap.put(jobj.get("token").toString(),
						"[{  \"complexty\": \"regular\",   \"discqty\": \"0\",    \"exch\": \"+exchangeStr+\",    \"pCode\": \"MIS\",    \"prctyp\": \"MKT\",    \"price\": \"0.0\",    \"qty\": "
								+ jobj.get("lot_size") + ",    \"ret\": \"DAY\",    \"symbol_id\":\""
								+ jobj.get("token") + "\", \"trading_symbol\": \"" + jobj.get("trading_symbol")
								+ "\",  \"transtype\": \"BUY\",   \"trigPrice\": \"0\",   \"orderTag\": \"order1\"       }]");
				;

				inputStr = jobj.get("token") + "="
						+ "[    {        \"exchSeg\": \"nse_fo\",        \"pCode\": \"MIS\",        \"netQty\": \""
						+ jobj.get("lot_size") + "\",        \"tockenNo\": \"" + jobj.get("token")
						+ "\",        \"symbol\": \"FINNIFTY\"    }]";

				cancelFile.write(inputStr);

			}

			// 46461=[ { "exchSeg": "nse_fo", "pCode": "MIS", "netQty": "50", "tockenNo":
			// "46461", "symbol": "FINNIFTY" }]
			optionsFile.close();
			orderFile.close();
			cancelFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void setup() throws NoSuchAlgorithmException {
		// TODO Auto-generated constructor stub
		String s1 = "1194239"
				+ "zTiFVe1AZzQIWTHiN0O8OrV3E2akEXmND9hTAE0949LAJog2vx4Oazl18CiO2vgRyW7Wlz0W1DI9TWskgCugla9HAWHxJGZoLH7WAJyniVQ1m8rUAWm4t24VdROdlNyQ"
				+ getAPIEncriptionString();
		KEY = toHexString(getSHA(s1));
		sessionId = getUserSID();
		KEY = "Bearer 1194239 " + sessionId;

		try {
			invalidateSessionID();
			websocketSessionID();
			webSocket = connectToWS();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public void handleMessage(String message) {
		/*
		 * if (toJsonObject(message).containsKey("lp")) {
		 * (submittedOrdersMap.get(toJsonObject(message).get("tk").toString()))
		 * .tradeActionAt(Double.parseDouble(toJsonObject(message).get("lp").toString())
		 * ); System.out.println(
		 * "                                                                                                                            "
		 * + toJsonObject(message).get("lp")); }
		 * 
		 */
		priceInfo.add(message);
		// TradeMontor.lock.notifyAll();

	}

	public static void processMessage(String message) {

		if (toJsonObject(message).containsKey("lp")) {
			(submittedOrdersMap.get(toJsonObject(message).get("tk").toString()))
					.tradeActionAt(Double.parseDouble(toJsonObject(message).get("lp").toString()));
			System.out.println(
					"                                                                                                                            "
							+ toJsonObject(message).get("lp"));
		}

	}

	private WebSocket connectToWS() {
		try {

			WebSocket webSocket = new WebSocketFactory().createSocket("wss://ws1.aliceblueonline.com/NorenWS/")
					.addListener(new WebSocketAdapter() {
						@Override
						public void onTextMessage(WebSocket ws, String message) {
							// handleMessage(message);
							while (true) {
								priceInfo.add(message);
								System.out.println("Processing>>>>" + message);
							}
						}
					}).connect();

			String susertoken = "";
			try {
				susertoken = toHexString(getSHA(toHexString(getSHA(sessionId))));
				webSocketPayload = "{\r\n" + "    \"susertoken\": \"" + susertoken + "\",\r\n" + "    \"t\": \"c\",\r\n"
						+ "    \"actid\": \"1194239_API\",\r\n" + "    \"uid\": \"1194239_API\",\r\n"
						+ "    \"source\": \"API\"\r\n" + "}";
				System.out.println(webSocketPayload);
				webSocket.sendText(webSocketPayload);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return webSocket;
		} catch (WebSocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void submitToken(String stockTokenStr) {
		webSocket.sendText(webSocketPayload);
		webSocket.sendText(stockTokenStr);
	}

	public static String getUserSID() {
		HttpResponse<String> response = null;
		Unirest.setTimeouts(0, 0);
		String jsonStr = "{\r\n        \"userId\": \"1194239\",\r\n        \"userData\": \"" + KEY + "\"\r\n     } ";
		try {
			response = Unirest.post("https://ant.aliceblueonline.com/rest/AliceBlueAPIService/api/customer/getUserSID")
					.header("Content-Type", "application/json").body(jsonStr).asString();
		} catch (UnirestException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		JSONParser parser = new JSONParser();
		JSONObject json = null;
		try {
			json = (JSONObject) parser.parse(response.getBody());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return (String) json.get("sessionID");
	}

	public static String getAPIEncriptionString() {
		HttpResponse<String> response = null;
		Unirest.setTimeouts(0, 0);
		try {
			response = Unirest
					.post("https://ant.aliceblueonline.com/rest/AliceBlueAPIService/api/customer/getAPIEncpkey")
					.header("Content-Type", "application/json").body("{\r\n    \"userId\":\"1194239\"\r\n}").asString();
		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JSONParser parser = new JSONParser();
		JSONObject json = null;
		try {
			json = (JSONObject) parser.parse(response.getBody());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return (String) json.get("encKey");
	}

	public static byte[] getSHA(String input) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		return md.digest(input.getBytes(StandardCharsets.UTF_8));
	}

	public static String toHexString(byte[] hash) {
		BigInteger number = new BigInteger(1, hash);
		StringBuilder hexString = new StringBuilder(number.toString(16));
		while (hexString.length() < 64) {
			hexString.insert(0, '0');
		}
		return hexString.toString();
	}

	public String invalidateSessionID() throws Exception {

		HttpResponse<String> response = null;
		Unirest.setTimeouts(0, 0);
		try {

			response = Unirest
					.post("https://ant.aliceblueonline.com/rest/AliceBlueAPIService/api/ws/invalidateSocketSess")
					.header("Authorization", KEY).body(" {\"loginType\":\"API\"}").asString();

		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return response.getBody();

	}

	public String websocketSessionID() throws Exception {

		HttpResponse<String> response = null;
		Unirest.setTimeouts(0, 0);
		try {

			response = Unirest.post("https://ant.aliceblueonline.com/rest/AliceBlueAPIService/api/ws/createSocketSess")
					.header("Authorization", KEY).body(" {\"loginType\":\"API\"}").asString();
		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return response.getBody();

	}

	public String getFunds() {

		HttpResponse<String> response = null;
		Unirest.setTimeouts(0, 0);
		try {

			response = Unirest.get("https://ant.aliceblueonline.com/rest/AliceBlueAPIService/api/limits/getRmsLimits")
					.header("Authorization", KEY).asString();

		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return response.getBody();

	}

	public static JSONObject toJsonObject(String jsonStr) {
		JSONObject jsonObject = null;
		try {
			JSONParser parser = new JSONParser();
			jsonObject = (JSONObject) parser.parse(jsonStr);
		} catch (JSONException | ParseException err) {
			err.printStackTrace();
		}
		return jsonObject;
	}

	public static void sendMessage(String payLoad) {

		try {
			webSocket.sendText(payLoad);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {

		TradeMontor tradeMontor = new TradeMontor();
		sendMessage("   {\r\n" + "       \"k\":\"\",\r\n" + "       \"t\":\"h\"\r\n" + "}");
		Iterator<String> iterator = placeorderMap.keySet().iterator();
		String stockIndex = "";

		while (iterator.hasNext()) {
			stockIndex = (String) iterator.next();
			if (submittedOrdersMap.get(stockIndex) == null) {
				submittedOrdersMap.put(stockIndex, new PriceHandler(KEY, stockIndex));
			}
			org.json.JSONArray array = new org.json.JSONArray(placeorderMap.get(stockIndex));
			org.json.JSONObject stockDetails = (org.json.JSONObject) array.get(0);
			// System.out.println(stockDetails.get("symbol_id"));
			// {"k":"NSE|26000","t":"t"}
			String requestMsg = "{\"k\":\"" + exchangeStr + "|" + stockDetails.get("symbol_id") + "\",\"t\":\"t\"}";
			if (requestMsg.contains("NFO")) {
				submitToken(requestMsg);
				Thread.sleep(5000);
			}
		}

		// get the property value and print it out

		latch.await();

	}

}
