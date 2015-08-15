package io.github.deton.yoteihyo;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.*;
import java.util.logging.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
import microsoft.exchange.webservices.data.*;

@WebServlet(name="YoteihyoServlet",urlPatterns={"/yoteihyo"})
public class YoteihyoServlet extends HttpServlet {
    static Logger logger = Logger.getLogger("YoteihyoServlet");
    final static String server = LocalProperties.server;
    final static String userId = LocalProperties.userId;
    final static String password = LocalProperties.password;
    ExchangeClient exchange = new ExchangeClient(server, userId, password);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        resp.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            String emails = req.getParameter("emails");
            String json = getAppointments(Arrays.asList(emails.split(",")));
            out.print(json);
        } catch (Exception ex) {
            try {
                resp.sendError(resp.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
            } catch (IOException ioex) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "on sendError()", ioex);
                }
            }
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "doGet", ex);
                if (ex instanceof ServletException) {
                    logger.log(Level.WARNING, "rootCause",
                            ((ServletException)ex).getRootCause());
                }
            }
        }
    }

    /**
     * Exchangeから予定を取得してJSON文字列化する
     * @param emails 予定取得対象emailアドレスのリスト
     * @return 取得した予定のJSON文字列
     * @exception ServiceLocalException EWS API呼び出し時のException
     */
    String getAppointments(List<String> emails) throws Exception {
        // 明日の予定まで取得
        // TODO: 次の営業日まで
        // XXX: getCalendarEventsの場合、1日ぶんだと当日分のみ全て取得
        // XXX: 24時間未満だとException
        long now = System.currentTimeMillis();
        final long oneDayMs = 2 * 24 * 60 * 60 * 1000;
        Date startDate = new Date(now);
        Date endDate = new Date(now + oneDayMs);
        StringBuilder sb = new StringBuilder("{");
        for (String email : emails) {
            getAppointments(sb, email, startDate, endDate);
        }
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1); // delete ',' at end
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Exchangeから予定を取得してJSON文字列化する
     * @param sb 出力先
     * @param email 予定取得対象emailアドレス
     * @exception ServiceLocalException EWS API呼び出し時のException
     */
    void getAppointments(StringBuilder sb, String email, Date startDate,
            Date endDate) throws Exception {
        Collection<CalendarEvent> calendarEvents;
        List<String> emails = new ArrayList<String>();
        emails.add(email);
        calendarEvents = exchange.getCalendarEvents(emails, startDate, endDate);
        if (calendarEvents == null) {
            throw new YoteihyoException("Failed to get appointments from Exchange");
        }
        sb.append("\"");
        sb.append(email.replace("\"", "\\\""));
        sb.append("\":[");
        for (CalendarEvent a : calendarEvents) {
            sb.append("{");
            sb.append("\"startTime\":");
            sb.append(a.getStartTime().getTime() / 1000); // [sec]
            sb.append(",\"endTime\":");
            sb.append(a.getEndTime().getTime() / 1000);
            CalendarEventDetails details = a.getDetails();
            if (details == null) {
                sb.append(",\"subject\":null,\"location\":null");
            } else {
                String subj = details.getSubject();
                sb.append(",\"subject\":");
                if (subj == null) {
                    sb.append("null");
                } else {
                    sb.append("\"");
                    sb.append(subj.replace("\"", "\\\""));
                    sb.append("\"");
                }
                String loc = details.getLocation();
                sb.append(",\"location\":");
                if (loc == null) {
                    sb.append("null");
                } else {
                    sb.append("\"");
                    sb.append(loc.replace("\"", "\\\""));
                    sb.append("\"");
                }
            }
            sb.append(",\"freeBusyStatus\":\"");
            switch (a.getFreeBusyStatus()) {
            case Free:
                sb.append("Free");
                break;
            case Tentative:
                sb.append("Tentative");
                break;
            case Busy:
                sb.append("Busy");
                break;
            case OOF: // Out Of Office
                sb.append("OOF");
                break;
            case NoData:
            default:
                sb.append("NoData");
                break;
            }
            sb.append("\"},");
        }
        if (sb.charAt(sb.length() - 1) == ',') { // maybe '[' if empty
            sb.deleteCharAt(sb.length() - 1); // delete ',' at end
        }
        sb.append("],");
    }
}
