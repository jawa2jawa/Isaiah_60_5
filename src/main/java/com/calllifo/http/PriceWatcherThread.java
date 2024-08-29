package com.calllifo.http;

public class PriceWatcherThread implements Runnable {

	public PriceWatcherThread() {
	}

	@Override

	public void run() {

		while (true) {
			if (!TradeMontor.priceInfo.isEmpty()) {
				try {
					String message = TradeMontor.priceInfo.take();
					if(!message.equals("{\"t\": \"ck\",\"s\": \"OK\",\"uid\":\"1194239_API\"}")) {
						org.json.JSONObject stockDetails = new org.json.JSONObject(message);
						if(stockDetails.get("t")=="tf") {
							System.out.println("Feed>>>>"+message);
						}
						
					}
					//TradeMontor.processMessage(message);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

}