package unisa;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
/**
 * 
 * @author gianluigi
 *
 */
public class MyFormatter extends Formatter {

	/**
	 * Classe che estende Formatter
	 * 
	 * Creata dall'esigenza di avere un log personalizzato e personalizzabile in futuro
	 * La classe MyFormatter permette di dare l'output personalizzato al logger
	 */

	//private static final DateFormat df = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

	@Override
	public String format(LogRecord record) {
		// TODO Auto-generated method stub
		StringBuilder builder = new StringBuilder(1000);
		//builder.append(df.format(new Date(record.getMillis()))).append(" - ");
		//		builder.append("[");
		builder.append(Long.toString(new Date().getTime())).append("\t");
		//builder.append("[").append(record.getSourceClassName()).append(".");
		//builder.append(record.getSourceMethodName()).append("] - ");
		builder.append(record.getLevel()).append("\t");
		builder.append(formatMessage(record));
		builder.append("\n");
		return builder.toString();
	}
	@Override
	public String getHead(Handler h) {
		return super.getHead(h);
	}

	@Override
	public String getTail(Handler h) {
		return super.getTail(h);
	}
}
