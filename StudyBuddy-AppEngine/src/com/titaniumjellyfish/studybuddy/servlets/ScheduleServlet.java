package com.titaniumjellyfish.studybuddy.servlets;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.titaniumjellyfish.studybuddy.DataProcessor;

public class ScheduleServlet extends BaseServlet {

	private static final Logger log = Logger.getLogger(ScheduleServlet.class.getName());
	
	private static final long serialVersionUID = -5729055583360656573L;
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp){
		// this was called by the scheduler defined in cron.xml. it has no data.
		log.info("ScheduleServlet#doGet was called");
		DataProcessor.reprocessAll();
	}
}
