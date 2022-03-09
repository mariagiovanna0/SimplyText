package unisa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.XmlResourceParser;

public class TaskParser {

	private List<Task> tasks;
	private String is;

	public TaskParser() {
		super();
		tasks = new ArrayList<Task>();
	}
	
	public static String convertStreamToString(InputStream is)	throws IOException {

		if (is != null) {
			Writer writer = new StringWriter();
			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(
						new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				is.close();
			}
			return writer.toString();
		} else {        
			return "";
		}
	}
	
	public List<Task> parse(XmlResourceParser parser){
		
		Task current = null;
		
		try {
			int eventType = parser.getEventType();

			while (eventType != XmlPullParser.END_DOCUMENT) {
				String name = null;

				switch (eventType){
				case XmlPullParser.START_TAG:
					name = parser.getName().toLowerCase();

					if (name.equals("task")) {
						String title = parser.getAttributeValue(0);
						current = new Task(title);
						tasks.add(current);
					}
					
					if (name.equals("start")) {
						current.setStart(parser.nextText());
					}
					
					if (name.equals("end")) {
						current.setEnd(parser.nextText());
					}

					break;
				case XmlPullParser.END_TAG:
					name = parser.getName();
					break;
				}

				eventType = parser.next();
			}
		}
		catch (XmlPullParserException e) {
			throw new RuntimeException("Cannot parse XML");
		}
		catch (IOException e) {
			throw new RuntimeException("Cannot parse XML");
		}
		finally {
			parser.close();
		}
		
		return tasks;
	}

//	public List<Task> parse2(){
//
//		Log.d("ciao", "Inizio il parsing");
//		DocumentBuilder builder;
//		try {
//			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//			Document doc = builder.parse(is);	
//			NodeList taskNodes = doc.getElementsByTagName("task");
//			Log.d("ciao", "numero di task: "+taskNodes.getLength());
//			for (int i=0;i<taskNodes.getLength();i++) {
//				Task task = new Task();
//				Element taskEl = (Element)taskNodes.item(i);
//				task.setStart(taskEl.getChildNodes().item(0).getTextContent());
//				task.setEnd(taskEl.getChildNodes().item(1).getTextContent());
//				tasks.add(task);
//			}
////			is.close();
//
//
//		} catch (ParserConfigurationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SAXException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		return tasks;
//	}


}
