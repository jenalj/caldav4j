/**
 * test GenerateQuery
 * TODO this class will never fail test: you've to
 *    check resulting output visually and verify if
 *    it's the expected result 
 */
package com.github.caldav4j.util;

import com.github.caldav4j.BaseTestCase;
import com.github.caldav4j.CalDAVConstants;
import com.github.caldav4j.exceptions.CalDAV4JException;
import com.github.caldav4j.exceptions.DOMValidationException;
import com.github.caldav4j.methods.PutGetTest;
import com.github.caldav4j.model.request.*;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class GenerateQueryTest extends BaseTestCase {
    private static final Logger log = LoggerFactory.getLogger(PutGetTest.class);
 
    @Before
    @Override
    public void setUp() throws Exception {}

    @After
    @Override
    public void tearDown() throws Exception {}
    
    private String printQuery(CalendarQuery query) {
        try {
    		query.validate();
        	Document doc = query.createNewDocument();
			return XMLUtils.toPrettyXML(doc);

        } catch (DOMValidationException domve) {
            log.error("Error trying to create DOM from CalDAVReportRequest: ", domve);
            throw new RuntimeException(domve);
        } catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
		return null;
		
    }
    
    
    /**
     * basic VEVENT CompFilter
     */
    @Test
    public void testFilter_VEVENT() {
		try {    	
			log.info("Filter: VEVENT");
			GenerateQuery gq = new GenerateQuery();		
			gq.setFilter("VEVENT");
			

			log.info(printQuery(gq.generate()));
			
			// and now test the constructor
			gq = new GenerateQuery(null,Component.VEVENT);		
			
			log.info(printQuery(gq.generate()));
		} catch (CalDAV4JException e) {
			e.printStackTrace();
			assertTrue(false);
		} 

    }
@Test
    public void testComp_VEVENT() {

		try {
			log.info("Comp: VEVENT");
			GenerateQuery gq = new GenerateQuery();		
			gq.setComponent(Component.VEVENT);
	
			log.info("Set component:\n" + printQuery(gq.generate()));
			
			// and now test the constructor
			final String VEVENT_PROPERTIES_BY_TIMERANGE_F = "VEVENT [20060104T000000Z;20060105T000000Z]";
			final String VEVENT_PROPERTIES_BY_TIMERANGE_C = "VEVENT : SUMMARY,UID,DTSTART,DTEND,RRULE,RDATE,DURATION,EXRULE,EXDATE,RECURRENCE-ID";
			gq = new GenerateQuery(VEVENT_PROPERTIES_BY_TIMERANGE_C, VEVENT_PROPERTIES_BY_TIMERANGE_F);		
		
			// print the query generated by the constructor
			log.info("Constructor:\n" + printQuery(gq.generate()));
			
			// save the query
			CalendarQuery query = gq.generate();
			
			// modify to conform RFC example 7.8.1. 
			//    Example: Partial Retrieval of Events by Time Range
			Comp compVtimezone = new Comp();
			compVtimezone.setName(Component.VTIMEZONE);
			query.getCalendarDataProp().getComp().getComps().add(compVtimezone);
			log.info("calendar-data with VTIMEZONE:\n" + printQuery(query));
			
			// now remove calendar data
			gq.setNoCalendarData(true);			
			log.info("no calendar-data:\n" + printQuery(gq.generate()));			
			
			// and limit recurrence set
			gq.setNoCalendarData(false);			
			gq.setRecurrenceSet("20060101T170000Z","20060105T235900Z", CalendarData.EXPAND);
			// gq.setRecurrenceSet("20060104T000000Z","20060105T000000Z");
			log.info("limit-recurrence-set:\n" + printQuery(gq.generate()));
		} catch (CalDAV4JException e) {
			e.printStackTrace();
			assertTrue(false);
		} 

    }    

@Test
public void testDateTime() throws ParseException {
	String dates[] = new String[] {  "20060101T170000Z","20060105T230000Z" };
	
	for (String d : dates) {
		log.info((new DateTime(d)).toString());
	}
}
    // Creating Calendar-Query like the RFC's one
@Test
    public void testQuery_TODO()  {
		try {
			List<String> a = new ArrayList<>();
			a.add("STATUS!=CANCELLED");
			a.add("COMPLETED==UNDEF");
			a.add("DTSTART==[;20080810]");
			
	
			GenerateQuery gq = new GenerateQuery();	
			gq.setFilter("VTODO", a);

			log.info(printQuery(gq.generate()));
			
			// and now the constructor
			String fquery = "VTODO : STATUS!=CANCELLED , COMPLETED==UNDEF , DTSTART==[;20080810]";
			gq = new GenerateQuery(null, fquery);	

			log.info(printQuery(gq.generate()));
			
			// and now the constru ctor
			fquery = "VTODO [20060106T100000Z;20060106T100000Z]: STATUS!=CANCELLED , COMPLETED==UNDEF , DTSTART==[;20080810]";
			gq = new GenerateQuery(null, fquery);	

			log.info(printQuery(gq.generate()));
			
		} catch (CalDAV4JException e) {
			e.printStackTrace();
			assertTrue(false);
		}	
    }

    @Test
    public void testQuery_ATTENDEE()  {
		try {
			log.info("VEVENT + ATTENDEE:");
			List<String> a = new ArrayList<>();
			a.add("ATTENDEE==mailto:lisa@example.com");
			
			GenerateQuery gq = new GenerateQuery();
			gq.setFilter("VEVENT", a);
		
			log.info("setFilter()"+printQuery(gq.generate()));
			
			gq = new GenerateQuery(null,"VEVENT : ATTENDEE==mailto:lisa@example.com");	
		
			log.info("Constructor:"+printQuery(gq.generate()));			
		} catch (CalDAV4JException e) {
			e.printStackTrace();
			assertTrue(false);
		} 		
    }

    /**
     * queries for nested components: there's no syntax for this, 
     *  so you have to use a bit of magic 
     * VTODO : VALARM 
     */
    @Test
    public void testQuery_VALARM()  {
		try {
			CalendarQuery compoundQuery = null;
			
			// create the external comp-filter
			GenerateQuery gq = new GenerateQuery();	
			gq.setFilter("VTODO", null);
			compoundQuery = gq.generate();
			log.info(printQuery(gq.generate()));
			
			// add the inner filter (VALARM in time-range) to the VTODO one
			gq = new GenerateQuery(null, "VALARM [20060106T100000Z;20060106T170000Z]");
			compoundQuery.getCompFilter().getCompFilters().get(0).addCompFilter(
					gq.generate().getCompFilter().getCompFilters().get(0)
					);
			
			log.info(printQuery(compoundQuery));
		} catch (CalDAV4JException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		
    }
    @Test
    public void testFilterProperties()  {
		try {
			List<String> a = new ArrayList<>();
			a.add("UID==DSDDAS123-D32423-42332-dasdsafwe");
			a.add("X-PLUTO-SPADA!=fsdfsdfds");
			a.add("SUMMARY!=CDSDafsd");
			a.add("DESCRIPTION==UNDEF");
			// a.add("DTSTART==[13082008,14082008]");
			
	
			GenerateQuery gq = new GenerateQuery();	
			gq.setFilter("VEVENT", a);

			log.info(printQuery(gq.generate()));
			
			String fquery = "VEVENT:UID==DSDDAS123-D32423-42332-dasdsafwe" 
				+ ",X-PLUTO-SPADA!=fsdfsdfds"
				+ ",SUMMARY!=CDSDafsd"
				+ ",DESCRIPTION==UNDEF";
				
			gq = new GenerateQuery(null, fquery);	
			log.info(printQuery(gq.generate()));
		} catch (CalDAV4JException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		
    }
    
    /**
     * @see <a href='http://tools.ietf.org/html/rfc4791#section-7.8.7'>RFC4791 7.8.7</a>
     */
    @Test
    public void testParamFilterProperties()  {
		try {

			GenerateQuery gq = new GenerateQuery();	

			
			String fquery = "VEVENT : ATTENDEE==mailto:lisa@example.com";
				
			gq = new GenerateQuery(null, fquery);	
			CalendarQuery query = gq.generate();
			log.info(printQuery(query));

			/*
			 * compose 
			 * <param-filter name="PARTSTAT"><text-match>NEEDS-ACTION</text-match></param-filter>
			 */
			ParamFilter paramSentBy = new ParamFilter();
			paramSentBy.setName("PARTSTAT");
			paramSentBy.setTextMatch(new TextMatch(null,null,null, "NEEDS-ACTION"));

			// append into ATTENDEE prop-filter
			query.getCompFilter().getCompFilters().get(0)
				.getPropFilters().get(0).addParamFilter(paramSentBy);
			
			log.info(printQuery(query));
		} catch (CalDAV4JException e) {
			e.printStackTrace();
			assertTrue(false);
		}
		
    }
    
    @Test
    public void constructor() throws Exception {
    	GenerateQuery gq = new GenerateQuery();
    	String s = XMLUtils.prettyPrint(gq.generate());
    	assertTrue(s.contains(CalDAVConstants.ELEM_ALLPROP));
    	log.info(s);
    	
    	gq = new GenerateQuery(null,null);
    	s = XMLUtils.prettyPrint(gq.generate());
    	assertTrue(s.contains(CalDAVConstants.ELEM_ALLPROP));

    	log.info(s);

    	gq = new GenerateQuery(Component.VTODO,null);
    	s = XMLUtils.prettyPrint(gq.generate());
    	log.info(s);

    	gq = new GenerateQuery(null,Component.VTODO);
    	assertTrue(s.contains(CalDAVConstants.ELEM_ALLPROP));

    	s = XMLUtils.prettyPrint(gq.generate());
    	log.info(s);
  }

}
