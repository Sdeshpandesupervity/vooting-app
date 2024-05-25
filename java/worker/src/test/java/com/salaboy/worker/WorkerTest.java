package com.salaboy.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DependsOn;
import org.springframework.test.context.TestPropertySource;

import com.salaboy.model.Results;
import com.salaboy.model.Vote;

import io.diagrid.springboot.dapr.core.DaprKeyValueTemplate;

import org.awaitility.Duration;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes={DaprTestConfig.class}, webEnvironment = WebEnvironment.RANDOM_PORT)
@ComponentScan("com.salaboy.worker")
@ComponentScan("io.dapr.springboot")
@DependsOn("keyValueTemplate")
@TestPropertySource(properties = {
    "dapr.query.indexName=VotesQueryIndex",
})
public class WorkerTest {
    
    @Autowired
	@Qualifier("votesKeyValueTemplate")
	DaprKeyValueTemplate votesKeyValueTemplate;

    @Autowired
	@Qualifier("resultsKeyValueTemplate")
	DaprKeyValueTemplate resultsKeyValueTemplate;

    @SpyBean
    WorkerJob workerJob;

    @Test
    public void testWorker() throws InterruptedException {


        Vote voteA = votesKeyValueTemplate.insert(new Vote("vote", "123-123", "a", "123-123" ));
		assertNotNull(voteA);
        System.out.println("Voted A");
        Vote voteB = votesKeyValueTemplate.insert(new Vote("vote", "456-456", "b", "456-456"));
		assertNotNull(voteB);
        System.out.println("Voted B");

        await()
          .atMost(Duration.FIVE_SECONDS)
          .untilAsserted(() -> verify(workerJob, atLeast(3)).work());

        Vote voteB2 = votesKeyValueTemplate.insert(new Vote("vote", "789-789", "b", "789-789"));
        assertNotNull(voteB2);
        System.out.println("Voted B2");

        await()
          .atMost(Duration.FIVE_SECONDS)
          .untilAsserted(() -> verify(workerJob, atLeast(3)).work());
        
        Thread.sleep(5000);

		Optional<Results> resultsOptional = resultsKeyValueTemplate.findById("results", Results.class);
        assertTrue(resultsOptional.isPresent());
        Results results = resultsOptional.get();
        assertEquals(Integer.valueOf(1), results.getOptionA());
        assertEquals(Integer.valueOf(2), results.getOptionB());
        
        
    }
}
