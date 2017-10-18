package city.saopaulo;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Get SP data and send it to a port
 * 
 * @author veruska
 */

public class GPSDataSP {

	private static final String FILE_SEPARATOR = ",";
	private static final int INTERVAL = 1000; // 1 second

	private static final String URL_POST_SAOPAULO = "";

	private static String key1SP = "";
	private static String key2SP = "";
	private static String LINK_WEB_SP = "";

	public static void main(String args[]) throws IOException {

		if (args.length != 1) {
			System.out.println("Usage: <port>");
			System.exit(1);
		}
		
		System.out.println("Starting SP crawler...");

		new Thread(new Runnable() {

			@Override
			public void run() {
				int port = Integer.valueOf(args[0]);

				ServerSocket srvr = null;
				Socket skt = null;
				PrintWriter out = null;

				DefaultHttpClient dHClient = new DefaultHttpClient();
				String lastKey = key1SP;

				HttpPost post = new HttpPost(URL_POST_SAOPAULO + lastKey);
				ResponseHandler<String> handler = new BasicResponseHandler();
				String resp = null;
				try {
					resp = dHClient.execute(post, handler);

					System.out.println(resp);
				} catch (IOException e) {
					resp = null;
					System.err.print("Permission denied. ");
				}

				String responseTimestamp = "";
				Long gpsPointID = 0L;

				boolean finish = false;

				while (!finish) {

					if (srvr == null) {
						try {
							srvr = new ServerSocket(port);
							skt = srvr.accept();

							out = new PrintWriter(skt.getOutputStream(), true);
						} catch (ConnectException e) {
							System.err.println(e.getMessage());
						} catch (UnknownHostException e) {
							System.err.println(e.getMessage());
						} catch (IOException e) {
							System.err.println(e.getMessage());
						}
					}

					if (out != null) {

						try {

							HttpGet get = null;
							String jsonText = null;

							if (resp != null) {
								try {
									get = new HttpGet(LINK_WEB_SP);
									jsonText = dHClient.execute(get, handler);
								} catch (ClientProtocolException e) {
									System.out.println(e.getMessage()
											+ " line 121 ");
								} catch (IOException e) {
									System.out.println(e.getMessage()
											+ " line 124 ");
								}
							}

							/**
							 * changes the key when get is null
							 **/
							if (jsonText == null || !jsonText.startsWith("{")) {
								System.out.println(jsonText);
								if (lastKey.equals(key1SP)) {
									lastKey = key2SP;
								} else {
									lastKey = key1SP;
								}

								post = new HttpPost(URL_POST_SAOPAULO + lastKey);

								try {
									resp = dHClient.execute(post, handler);

									System.out.println(resp + " - " + lastKey);
								} catch (IOException e) {
									resp = null;
									System.err.print("Permission denied. ");
								}

								if (resp != null) {
									try {
										get = new HttpGet(LINK_WEB_SP);
										jsonText = dHClient.execute(get,
												handler);
									} catch (ClientProtocolException e) {
										System.out.println(e.getMessage()
												+ " line 159 ");
									} catch (IOException e) {
										System.out.println(e.getMessage()
												+ " line 162 ");
									}
								}
							}

							if (jsonText != null && jsonText.startsWith("{")) {
								JSONObject json = new JSONObject(jsonText);
								JSONArray locatedLines = json.getJSONArray("l");
								String firstVehicle = locatedLines
										.getJSONObject(0).getJSONArray("vs")
										.getJSONObject(0).getString("ta");
								String currentResponseTimestamp = firstVehicle
										.substring(0, 10);
								Integer hourTemp = Integer.valueOf(firstVehicle
										.substring(11, 13));

								// Creates a new file for each day
								if (!currentResponseTimestamp
										.equals(responseTimestamp)
										&& hourTemp >= 3) {

									responseTimestamp = currentResponseTimestamp;
									gpsPointID = 0L;
								}

								for (int i = 0; i < locatedLines.length(); i++) {
									JSONArray locatedVehicles = locatedLines
											.getJSONObject(i)
											.getJSONArray("vs");

									for (int j = 0; j < locatedVehicles
											.length(); j++) {
										JSONObject vehicle = locatedVehicles
												.getJSONObject(j);

										Integer busCode = vehicle.getInt("p");
										Double lat = vehicle.getDouble("py");
										Double lng = vehicle.getDouble("px");
										String time = vehicle.getString("ta")
												.substring(11);
										time = convertUTCTime(time.substring(0,
												time.length() - 1));
										String route = locatedLines
												.getJSONObject(i)
												.getString("c");
										Long pointId = gpsPointID++;

										String output = busCode
												+ FILE_SEPARATOR + lat
												+ FILE_SEPARATOR + lng
												+ FILE_SEPARATOR + time
												+ FILE_SEPARATOR + route
												+ FILE_SEPARATOR + pointId;

										out.println(output);
									}
								}
							}

						} catch (JSONException e) {
							System.out.println(e + "line 234");
						}
					}
					
					try {
						Thread.sleep(INTERVAL);
					} catch (InterruptedException e) {
						System.err.println(e.getMessage());
					}
				}
			}
		}).start();
	}

	private static String convertUTCTime(String utcTime) {
		int hour = Integer.valueOf(utcTime.substring(0, 2));
		int newHour;
		switch (hour) {
		case 00:
			newHour = 21;
			break;

		case 01:
			newHour = 22;
			break;

		case 02:
			newHour = 23;
			break;

		default:
			newHour = hour - 3;
			break;
		}

		return newHour + utcTime.substring(2);
	}
}