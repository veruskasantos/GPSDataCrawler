package city.newyork;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Get NY bus data and send it to a port
 * 
 * @author veruska
 */

public class GPSDataNY {

	private static final String FILE_SEPARATOR = ",";
	private static final int INTERVAL = 1000; // 1 second

	private static String key1NY = "";
	private static String key2NY = "";
	private static String WEB_LINK_NY = "";

	public static void main(String args[]) throws IOException {

		if (args.length != 1) {
			System.out.println("Usage: <port>");
			System.exit(1);
		}

		System.out.println("Starting NY crawler...");

		new Thread(new Runnable() {

			@Override
			public void run() {
				int port = Integer.valueOf(args[0]);

				ServerSocket srvr = null;
				Socket skt = null;
				PrintWriter out = null;
				BufferedReader br = null;

				String lastKeyNY = key1NY;
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

							String jsonText = null;
							try {
								URL url = new URL(WEB_LINK_NY + lastKeyNY);
								URLConnection con = url.openConnection();
								InputStream is = con.getInputStream();
								br = new BufferedReader(new InputStreamReader(is));

								jsonText = readAll(br);
							} catch (MalformedURLException e) {
								System.out.println(e.getMessage());
							} catch (IOException e) {
								System.out.println(e.getMessage());
							}

							/**
							 * changes the key when get is null
							 **/
							if (jsonText == null || !jsonText.startsWith("{")) {
								System.out.println(jsonText);

								if (lastKeyNY.equals(key1NY)) {
									lastKeyNY = key2NY;
								} else {
									lastKeyNY = key1NY;
								}

								try {
									URL url = new URL(WEB_LINK_NY + lastKeyNY);
									URLConnection con = url.openConnection();
									InputStream is = con.getInputStream();
									br = new BufferedReader(new InputStreamReader(is));

									jsonText = readAll(br);
								} catch (MalformedURLException e) {
									System.out.println(e.getMessage());
								} catch (IOException e) {
									System.out.println(e.getMessage());
								}
							}

							if (jsonText != null && jsonText.startsWith("{")) {
								JSONObject serviceDelivery = new JSONObject(jsonText).getJSONObject("Siri")
										.getJSONObject("ServiceDelivery");

								String currentResponseTimestamp = serviceDelivery.getString("ResponseTimestamp")
										.substring(0, 10);
								JSONArray vehicleActivity = ((JSONObject) serviceDelivery
										.getJSONArray("VehicleMonitoringDelivery").get(0))
												.getJSONArray("VehicleActivity");

								// Creates a new file for each day
								if (!currentResponseTimestamp.equals(responseTimestamp)) {

									responseTimestamp = currentResponseTimestamp;
									gpsPointID = 0L;
								}

								for (int i = 0; i < vehicleActivity.length(); i++) { // information of each vehicle
									JSONObject monitoredVehicleJourney = vehicleActivity.getJSONObject(i)
											.getJSONObject("MonitoredVehicleJourney");

									String vehicleRef = monitoredVehicleJourney.getString("VehicleRef");
									JSONObject vehicleLocation = monitoredVehicleJourney
											.getJSONObject("VehicleLocation");
									Double lat = vehicleLocation.getDouble("Latitude");
									Double lng = vehicleLocation.getDouble("Longitude");
									String recordedAtTime = vehicleActivity.getJSONObject(i)
											.getString("RecordedAtTime");
									String time = recordedAtTime.substring(11, recordedAtTime.lastIndexOf("."));
									String publishedLineName = monitoredVehicleJourney.getString("PublishedLineName");
									Long pointId = gpsPointID++;

									String output = vehicleRef + FILE_SEPARATOR + lat + FILE_SEPARATOR + lng
											+ FILE_SEPARATOR + time + FILE_SEPARATOR + publishedLineName
											+ FILE_SEPARATOR + pointId;

									out.println(output);
								}
							}

						} catch (JSONException e) {
							System.out.println(e);
						}
					}
					
					try {
						Thread.sleep(INTERVAL);
					} catch (InterruptedException e) {
						System.out.println(e.getMessage());
					}
				}
			}
		}).start();
	}
	
	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}
}