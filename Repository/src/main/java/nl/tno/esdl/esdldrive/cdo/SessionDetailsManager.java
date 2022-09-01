/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */

package nl.tno.esdl.esdldrive.cdo;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

class SessionDetailsManager {
	
	private static final Logger log = Logger.getLogger(SessionDetailsManager.class.getName());
	
	private final static TimeUnit TIMEUNIT_SECONDS = TimeUnit.SECONDS;
	private final static int CLEANUP_INTERVAL_SECONDS = 3600; // check every hour
	private final static int SESSION_TIMEOUT_SECONDS = 3600 * 12; // 12 hours
	
	public SessionDetailsManager(ConcurrentHashMap<String, SessionDetails> sessionMap) {
		ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "cdo-session-cleanup");
				return t;
			}
		});
		
		log.info("Scheduling CDO session cleanup at every " + CLEANUP_INTERVAL_SECONDS + " seconds");
		scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				long millis = System.currentTimeMillis();
				Iterator<Entry<String, SessionDetails>> iterator = sessionMap.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<String, SessionDetails> entry = iterator.next();
					if (entry.getValue().getLastUsed() + SESSION_TIMEOUT_SECONDS * 1000 < millis) {
						SessionDetailsManager.log.info("Removing session due to session timeout " + entry.getValue());
						entry.getValue().getCDOSession().close(); // close session
						iterator.remove();
					}
				}
			}
		}, CLEANUP_INTERVAL_SECONDS, CLEANUP_INTERVAL_SECONDS, TIMEUNIT_SECONDS);
	}
	
	
}