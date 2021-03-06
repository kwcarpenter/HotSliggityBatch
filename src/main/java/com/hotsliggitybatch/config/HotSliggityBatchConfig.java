package com.hotsliggitybatch.config;

import com.hotsliggitybatch.models.Match;
import com.hotsliggitybatch.models.MatchLog;
import com.hotsliggitybatch.tasklets.HotSliggityRequestTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class HotSliggityBatchConfig
{
    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Value("${input.resources}")
    private Resource[] resources;

    @Bean
    public Job hotSliggityBatchJob(@Qualifier("step1") Step step1, @Qualifier("step2") Step step2) {
        return jobBuilderFactory.get("hotSliggityBatchJob").start(step1).next(step2).build();
    }

    @Bean
    public Step step1(ItemReader<Match> itemReader, ItemProcessor<Match, MatchLog> itemProcessor) {
        return stepBuilderFactory.get("readAndProcessMatches").<Match, MatchLog> chunk(10)
                .reader(multiResourceItemReader()).processor(itemProcessor).build();
    }

    @Bean
    public Step step2(HotSliggityRequestTasklet hotSliggityRequestTasklet) {
        return stepBuilderFactory.get("buildRequest").tasklet(hotSliggityRequestTasklet).build();
    }

    @Bean
    public MultiResourceItemReader<Match> multiResourceItemReader() {
        MultiResourceItemReader<Match> resourceItemReader = new MultiResourceItemReader<Match>();
        resourceItemReader.setResources(resources);
        resourceItemReader.setDelegate(reader());

        return resourceItemReader;
    }

    public FlatFileItemReader<Match> reader() {
        FlatFileItemReader<Match> fileReader = new FlatFileItemReader<Match>();
        fileReader.setStrict(false);
        fileReader.setLineMapper(lineMapper());

        return fileReader;
    }

    private DefaultLineMapper<Match> lineMapper() {
        DefaultLineMapper<Match> lineMapper = new DefaultLineMapper<Match>();
        lineMapper.setLineTokenizer(delimitedLineTokenizer());
        lineMapper.setFieldSetMapper(fieldSetMapper());

        return lineMapper;
    }

    private DelimitedLineTokenizer delimitedLineTokenizer() {
        DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
        delimitedLineTokenizer.setStrict(false);
        delimitedLineTokenizer.setDelimiter(DelimitedLineTokenizer.DELIMITER_TAB);
        delimitedLineTokenizer.setNames(new String[] {
                "matchType",
                "mapName",
                "matchLength",
                "heroName",
                "heroLevel",
                "matchmakingRating",
                "ratingAdjustmentPoints",
                "matchDateTime"
        });

        return delimitedLineTokenizer;
    }

    private BeanWrapperFieldSetMapper<Match> fieldSetMapper() {
        BeanWrapperFieldSetMapper<Match> fieldSetMapper = new BeanWrapperFieldSetMapper<Match>();
        fieldSetMapper.setTargetType(Match.class);

        return fieldSetMapper;
    }
}