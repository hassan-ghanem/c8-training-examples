package org.example;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TimerBasedJobWorker {

    @Value("${zeebe.initial-timeout:30000}")
    private long initialTimeout;

    @Value("${zeebe.timeout-threshold:5000}")
    private long timeoutThreshold;
    
    private final ZeebeClient zeebeClient;

    public TimerBasedJobWorker(ZeebeClient zeebeClient) {
        this.zeebeClient = zeebeClient;
    }

    @JobWorker(type = "example-job", autoComplete = false, timeout = 30000)
    public void handleJob(final ActivatedJob job) {
        Timer timer = new Timer();
        AtomicBoolean jobCompleted = new AtomicBoolean(false);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
            	
            	if (!jobCompleted.get()) {
                    zeebeClient.newUpdateJobCommand(job.getKey())
                               .updateTimeout(initialTimeout)
                               .send()
                               .join();
                    System.out.println("Updated job timeout");
                }
            }
        }, initialTimeout - timeoutThreshold);

        try {
            
        	// Simulate job processing
            processJob();
            
            // Mark job as completed
            jobCompleted.set(true);

            // Complete the job
            zeebeClient.newCompleteCommand(job.getKey())
	            .send()
	            .exceptionally((throwable -> {
	                throw new RuntimeException("Could not complete job", throwable);
	            }));
            
            System.out.println("Job completed successfully");
            
        } finally {
        	
            timer.cancel(); // Ensure timer is canceled
        }
    }

    private void processJob() {
        try {
            Thread.sleep(35000); // Simulate long-running task
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
