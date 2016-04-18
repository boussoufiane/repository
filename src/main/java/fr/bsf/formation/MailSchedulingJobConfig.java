package fr.hubone.cooperationprojet.schedule.config;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import fr.hubone.cooperationprojet.dao.config.DaoAppConfig;
import fr.hubone.cooperationprojet.dao.model.Project;
import fr.hubone.cooperationprojet.schedule.ReminderMailJob;
import fr.hubone.cooperationprojet.services.IProjectService;


/**
 *  Configuration class for creating job dynamically on startup
 *  If the job already exist in DB it will be updated
 * @author yassine.boussoufiane
 * @date : 5 avr. 2016
 *
 */

@Configuration
//@ComponentScan(basePackages = {"fr.hubone.cooperationprojet.schedule"})
//@ComponentScan(basePackageClasses = CustomBatchConfigurer.class)
@EnableBatchProcessing
@Import({DaoAppConfig.class})
public class MailSchedulingJobConfig {
	
	private final static String JOB_NAME_PREFIX ="JOB_NAME_FOR_PROJECT_";
	private final static String JOB_GROUP_PREFIX ="JOB_GROUP_FOR_PROJECT_";
	private final static String  TRIGGER_NAME_PREFIX ="TRIGGER_NAME_FOR_PROJECT_";
	private final static String TRIGGER_GROUP_PREFIX ="TRIGGER_GROUP_FOR_PROJECT_";
	private final static String PROJECT_ID ="PROJECT_ID";
	
	

	@Autowired
	public IProjectService ProjectService;
	
	@Autowired
	public  ApplicationContext applicationContext;
	
	@Autowired
	private  DataSource dataSource ; 
	
	@Autowired
	//@Qualifier("")
	private JpaTransactionManager transactionManager ; 
	

	
	/**
	 * Bean for runing job of projects  
	 * @return
	 * @throws ParseException
	 * @throws SchedulerException
	 */
  @Bean 
  public SchedulerFactoryBean reminderMailSchedulerFactory() throws ParseException, SchedulerException{
	  SchedulerFactoryBean schedulerFactoryBean =  new SchedulerFactoryBean();
	  // there no need to specify dataSource because it will pick it up from spring context
	  schedulerFactoryBean.setDataSource(dataSource);
	  schedulerFactoryBean.setTransactionManager(transactionManager);
	  Properties props = new Properties();
	  props.setProperty("org.quartz.jobStore.selectWithLockSQL","SELECT * FROM {0}LOCKS UPDLOCK WHERE LOCK_NAME = ?");
	  schedulerFactoryBean.setQuartzProperties(props);
	  schedulerFactoryBean.setOverwriteExistingJobs(true);
      
      AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
      jobFactory.setApplicationContext(applicationContext);
      schedulerFactoryBean.setJobFactory(jobFactory);
  
		List<Project> projetcs = ProjectService.getProjectsWithActivatedReminderMail();
		List<Trigger> trigggers = new ArrayList();
		List<JobDetail> jobs = new ArrayList();
		  for(Project projet : projetcs){
			     Integer projectId = projet.getId();

				 JobDetail job = JobBuilder.newJob(ReminderMailJob.class)
						    .withIdentity(JOB_NAME_PREFIX + projectId, JOB_GROUP_PREFIX + projectId)
						    .storeDurably(true)	
						    .requestRecovery(true)
						    .build();
				 
				job.getJobDataMap().put(PROJECT_ID, projet.getId());
				jobs.add(job);
				 
				 Trigger trigger = TriggerBuilder
						 .newTrigger()
						 .withIdentity(TRIGGER_NAME_PREFIX + projectId , TRIGGER_GROUP_PREFIX + projectId)
						 .withSchedule(
						  CronScheduleBuilder.cronSchedule(projet.getReminderMail().getCronExpression()))
						 .forJob(job)
						  .startNow()
						 .build();
				 trigggers.add(trigger);
				 
			
		  }
		  schedulerFactoryBean.setJobDetails(jobs.toArray(new JobDetail[jobs.size()]));
		  schedulerFactoryBean.setTriggers( trigggers.toArray(new Trigger[trigggers.size()]));
		  schedulerFactoryBean.setAutoStartup(true);
	  
	  return schedulerFactoryBean ;
  }


  

  

  
  
}
