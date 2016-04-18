package fr.hubone.cooperationprojet.schedule;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.hubone.cooperationprojet.services.IMailService;

/**
 * Job for sending mail for collaborator of aa project
 * @author Yassine.BOUSSOUFIANE
 * @date : 6 avr. 2016
 *
 */

@Component
public class ReminderMailJob implements Job{
	
	@Autowired
	IMailService mailService;

	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		
		 JobDataMap data = context.getJobDetail().getJobDataMap();
		 
		 Integer projectId  =  data.getInt("PROJECT_ID");	

		 mailService.sendReminderMailToCollaboratorForProject(projectId);
	     
		
	}
	
	  
	
	
	


}
