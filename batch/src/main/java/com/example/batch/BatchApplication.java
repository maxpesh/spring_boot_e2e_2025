package com.example.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.BindException;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@SpringBootApplication
public class BatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }

    @Bean
    FlatFileItemReader<Dog> dogFlatFileItemReader(@Value("classpath:/dogs.csv") Resource resource) {
        return new FlatFileItemReaderBuilder<Dog>()
                .linesToSkip(1)
                .resource(resource)
                .name("dogsCsvToDbReader")
                .fieldSetMapper(fieldSet -> new Dog(fieldSet.readInt("id"),
                        fieldSet.readString("name"),
                        fieldSet.readString("owner"),
                        fieldSet.readString("description")))
                .delimited()
                .names("id,name,description,dob,owner,gender,image".split(","))
                .build();
    }

    @Bean
    JdbcBatchItemWriter<Dog> jdbcBatchItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Dog>()
                .dataSource(dataSource)
                .assertUpdates(true)
                .sql("insert into dog(id,name,description,owner) values(?,?,?,?)")
                .itemPreparedStatementSetter((dog, ps) -> {
                    ps.setInt(1, dog.id());
                    ps.setString(2, dog.name());
                    ps.setString(3, dog.description());
                    ps.setString(4, dog.owner());
                })
                .build();
    }

    @Bean
    Step csvToDbStep(JobRepository repo, PlatformTransactionManager trManager,
                     FlatFileItemReader<Dog> dogFlatFileItemReader,
                     JdbcBatchItemWriter<Dog> dogJdbcBatchItemWriter) {
        return new StepBuilder("dogCsvToDbStep", repo)
                .<Dog, Dog>chunk(10, trManager)
                .reader(dogFlatFileItemReader)
                .writer(dogJdbcBatchItemWriter)
                .build();
    }

    @Bean
    Job csvToDbJob(JobRepository repo, Step step) {
        return new JobBuilder("dogsCsvToDbJob", repo)
                .start(step)
                .incrementer(new RunIdIncrementer())
                .build();
    }
}

record Dog(int id, String name, String owner, String description) {
}