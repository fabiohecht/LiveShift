package net.liveshift.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.SimpleFormatter;


public class LogFormatter extends SimpleFormatter
{
        private final static DateFormat FORMATTER = new SimpleDateFormat("HH:mm:ss.SSS");
        private final static String SEPARATOR = "|";

        @Override
        public String format(java.util.logging.LogRecord record)
        {
                StringBuilder out = new StringBuilder();
                //out.append(Configuration.getDefaultPeerName());
                //out.append(SEPARATOR);
//                out.append(FORMATTER.format(new Date(record.getMillis())));
                out.append(FORMATTER.format(record.getMillis()));
                out.append(SEPARATOR);
                out.append(Thread.currentThread().getName());
                out.append(SEPARATOR);
                out.append(getFromLastDotOn(record.getSourceClassName()));
                out.append(SEPARATOR);
                out.append(record.getLevel());
                out.append(SEPARATOR);
                out.append(record.getSourceMethodName());
                out.append(SEPARATOR);
                out.append(record.getMessage());
                if (record.getThrown() != null)
                {
                        out.append(SEPARATOR);
                        out.append(record.getThrown());
                }
                out.append('\n');
                return out.toString();
        }

        private String getFromLastDotOn(String str)
        {
                int pos = str.lastIndexOf(".");
                return pos > 0 ? str.substring(pos + 1) : str;
        }
}
