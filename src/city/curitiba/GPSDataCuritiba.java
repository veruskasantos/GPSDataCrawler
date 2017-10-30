package city.curitiba;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Get Curitiba data and send it to a port
 * 
 * @author veruska
 */

public class GPSDataCuritiba {

	private static final String FILE_SEPARATOR = ",";
	private static final int INTERVAL = 120000; // 2 minutes

	private static String key1Curitiba = "";
	private static String LINK_WEB_CURITIBA = "";

	public static void main(String args[]) throws IOException {

		if (args.length != 1) {
			System.out.println("Usage: <port>");
			System.exit(1);
		}

		System.out.println("Starting Curitiba crawler...");

		new Thread(new Runnable() {

			@Override
			public void run() {
				int port = Integer.valueOf(args[0]);

				DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
				LocalDate localDate = LocalDate.now();
				String lastDate = dtf.format(localDate);

				ServerSocket srvr = null;
				Socket skt = null;
				PrintWriter out = null;

				DefaultHttpClient dHClient = new DefaultHttpClient();
				ResponseHandler<String> handler = new BasicResponseHandler();
				String resp = null;

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

							try {
								get = new HttpGet(LINK_WEB_CURITIBA + key1Curitiba);
								jsonText = dHClient.execute(get, handler);
							} catch (ClientProtocolException e) {
								System.out.println(e.getMessage());
							} catch (IOException e) {
								System.out.println(e.getMessage());
							}

							if (jsonText == null || jsonText.isEmpty()) {

								if (resp == null) {
									try {
										get = new HttpGet(LINK_WEB_CURITIBA + key1Curitiba);
										jsonText = dHClient.execute(get, handler);
									} catch (ClientProtocolException e) {
										System.out.println(e.getMessage());
									} catch (IOException e) {
										System.out.println(e.getMessage());
									}
								}
							}

							if (jsonText != null && !jsonText.isEmpty()) {
								JSONArray json = new JSONArray(jsonText);

								localDate = LocalDate.now();
								String currentDate = dtf.format(localDate);

								if (!currentDate.equals(lastDate)) {

									lastDate = currentDate;
									gpsPointID = 0L;
								}

								for (int i = 0; i < json.length(); i++) {

									JSONObject vehicle = json.getJSONObject(i);

									String busCode = vehicle.getString("PREFIXO");
									String lat = vehicle.getString("LAT").replace(FILE_SEPARATOR, ".");
									String lng = vehicle.getString("LON").replace(FILE_SEPARATOR, ".");
									String time = vehicle.getString("HORA");
									String route = vehicle.getString("LINHA");
									Long pointId = gpsPointID++;

									String output = busCode + FILE_SEPARATOR + lat + FILE_SEPARATOR + lng
											+ FILE_SEPARATOR + time + FILE_SEPARATOR + route + FILE_SEPARATOR + pointId;

									out.println(output);
								}
							}

						} catch (JSONException e) {
							System.out.println(e.getMessage());
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
}