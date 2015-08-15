package io.github.deton.yoteihyo;

import java.net.*;
import java.util.*;
import java.util.logging.*;
import microsoft.exchange.webservices.data.*;

public class ExchangeClient {
    static Logger logger = Logger.getLogger("ExchangeClient");

    // connection info for Exchange Server
    String server;
    String userId;
    String password;

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: ExchangeClient <server> <email> <password> <targetemail>");
            System.out.println("   ex: ExchagneClient exchange.example.com taro@example.com p@sSw0rD target-00309@example.com");
            return;
        }
        ExchangeClient ec = new ExchangeClient(args[0], args[1], args[2]);
        // XXX: findAppointments() throws Exception on getting appointments
        // of other user.
        //ec.outputAppointments(args[3]);
        List<String> emails = new ArrayList<String>();
        for (int i = 3; i < args.length; i++) {
            emails.add(args[i]);
        }
        ec.outputCalendarEvents(emails);
    }

    public ExchangeClient(String server, String userId, String password) throws IllegalArgumentException {
        if (server == null || userId == null || password == null) {
            throw new IllegalArgumentException("incomplete Exchange Server connection info: server=" + server + ",userId=" + userId);
        }
        this.server = server;
        this.userId = userId;
        this.password = password;
    }

    public void outputCalendarEvents(List<String> emails) throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date startDate = cal.getTime();
        cal.add(Calendar.DATE, 1);
        Date endDate = cal.getTime();
        Collection<CalendarEvent> calendarEvents = getCalendarEvents(emails, startDate, endDate);
        for (CalendarEvent a : calendarEvents) {
            System.out.println("Start: " + a.getStartTime());
            System.out.println("End: " + a.getEndTime());
            String subj = "-";
            String loc = "-";
            CalendarEventDetails details = a.getDetails();
            if (details != null) {
                subj = details.getSubject();
                loc = details.getLocation();
            }
            System.out.println("Subject: " + subj);
            System.out.println("Location: " + loc);
        }
    }

    public void outputAppointments(String targetAddress) throws Exception {
        FindItemsResults<Appointment> findResults = getAppointments(targetAddress);
        for (Appointment a : findResults.getItems()) {
            System.out.println("Start: " + a.getStart());
            System.out.println("End: " + a.getEnd());
            System.out.println("Subject: " + a.getSubject());
            System.out.println("Location: " + a.getLocation());
        }
    }

    public FindItemsResults<Appointment> getAppointments(String targetAddress) throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date startDate = cal.getTime();
        cal.add(Calendar.DATE, 1);
        Date endDate = cal.getTime();
        /*
        long now = System.currentTimeMillis();
        final long dayInMillis = 24 * 60 * 60 * 1000;
        Date startDate = new Date(now - dayInMillis);
        Date endDate = new Date(now + dayInMillis);
        */
        return getAppointments(targetAddress, startDate, endDate);
    }

    public FindItemsResults<Appointment> getAppointments(String targetAddress, Date startDate, Date endDate) throws Exception {
        ExchangeService exchangeService = createExchangeService();

        // http://blog.liris.org/2011/01/ms-exchange.html
        Mailbox target = new Mailbox(targetAddress);
        FolderId fid = new FolderId(WellKnownFolderName.Calendar, target);
        CalendarFolder cf = CalendarFolder.bind(exchangeService, fid);
        FindItemsResults<Appointment> findResults = cf.findAppointments(new CalendarView(startDate, endDate));
        return findResults;
    }

    ExchangeService createExchangeService() throws Exception {
        String serverUrl = "https://" + server + "/EWS/Exchange.asmx";
        ExchangeCredentials credentials = new WebCredentials(userId, password);
        ExchangeVersion exchangeVersion = ExchangeVersion.Exchange2010_SP2;
        ExchangeService exchangeService = new ExchangeService(exchangeVersion);
         
        exchangeService.setUrl(new URI(serverUrl));
        exchangeService.setCredentials(credentials);
        return exchangeService;
    }

    public Collection<CalendarEvent> getCalendarEvents(List<String> emails, Date startDate, Date endDate) throws Exception {
        try {
            ExchangeService service = createExchangeService();
            List<AttendeeInfo> attendees = new ArrayList<AttendeeInfo>();
            for (String email : emails) {
                attendees.add(new AttendeeInfo(email));
            }

            GetUserAvailabilityResults results = service.getUserAvailability(
                attendees, new TimeWindow(startDate, endDate),
                AvailabilityData.FreeBusy);

            for (AttendeeAvailability attendeeAvailability : results.getAttendeesAvailability()) {
                if (attendeeAvailability.getErrorCode() == ServiceError.NoError) {
                    return attendeeAvailability.getCalendarEvents();
                }
            }
        } catch (Exception ex) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "getCalendarEvents", ex);
            }
            throw ex;
        }
        return null;
    }
}
