package org.opencds.cqf.r4.config;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.opencds.cqf.common.config.HapiProperties;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.r4.providers.ActivityDefinitionApplyProvider;
import org.opencds.cqf.r4.providers.ApplyCqlOperationProvider;
import org.opencds.cqf.r4.providers.CacheValueSetsProvider;
import org.opencds.cqf.r4.providers.CodeSystemUpdateProvider;
import org.opencds.cqf.r4.providers.CqlExecutionProvider;
import org.opencds.cqf.r4.providers.LibraryOperationsProvider;
import org.opencds.cqf.r4.providers.MeasureOperationsProvider;
import org.opencds.cqf.r4.providers.ObservationProvider;
import org.opencds.cqf.r4.providers.PlanDefinitionApplyProvider;
import org.opencds.cqf.r4.providers.QuestionnaireProvider;
import org.opencds.cqf.tooling.library.r4.NarrativeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.ParserOptions;
import ca.uhn.fhir.cql.r4.provider.JpaTerminologyProvider;
import ca.uhn.fhir.jpa.config.BaseJavaConfigR4;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;

@Configuration
@ComponentScan(basePackages = "org.opencds.cqf.r4")
public class FhirServerConfigR4 extends BaseJavaConfigR4 {
    protected final DataSource myDataSource;

    @Autowired
    public FhirServerConfigR4(DataSource myDataSource) {
        this.myDataSource = myDataSource;
    }

    @Override
    public FhirContext fhirContextR4() {
        FhirContext retVal = FhirContext.forR4();

        // Don't strip versions in some places
        ParserOptions parserOptions = retVal.getParserOptions();
        parserOptions.setDontStripVersionsFromReferencesAtPaths("AuditEvent.entity.what");

        return retVal;
    }

    /**
     * We override the paging provider definition so that we can customize the
     * default/max page sizes for search results. You can set these however you
     * want, although very large page sizes will require a lot of RAM.
     */
    @Override
    public DatabaseBackedPagingProvider databaseBackedPagingProvider() {
        DatabaseBackedPagingProvider pagingProvider = super.databaseBackedPagingProvider();
        pagingProvider.setDefaultPageSize(HapiProperties.getDefaultPageSize());
        pagingProvider.setMaximumPageSize(HapiProperties.getMaximumPageSize());
        return pagingProvider;
    }

    @Override
    @Bean()
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean retVal = super.entityManagerFactory();
        retVal.setPersistenceUnitName(HapiProperties.getPersistenceUnitName());

        try {
            retVal.setDataSource(myDataSource);
        } catch (Exception e) {
            throw new ConfigurationException("Could not set the data source due to a configuration issue", e);
        }

        retVal.setJpaProperties(HapiProperties.getProperties());
        return retVal;
    }

    @Bean
    @Primary
    public JpaTransactionManager hapiTransactionManager(EntityManagerFactory entityManagerFactory) {
      JpaTransactionManager retVal = new JpaTransactionManager();
      retVal.setEntityManagerFactory(entityManagerFactory);
      return retVal;
    }

    @Bean(name= "myOperationProvidersR4")
    public List<Class<?>> operationProviders() {
        // TODO: Make this registry dynamic
        // Scan an interface, create a plugin-api, etc.
        // Basically, anything that's not included in base HAPI and implements an operation.
        List<Class<?>> classes = new ArrayList<>();
        classes.add(ActivityDefinitionApplyProvider.class);
        classes.add(ApplyCqlOperationProvider.class);
        classes.add(CacheValueSetsProvider.class);
        classes.add(CodeSystemUpdateProvider.class);
        classes.add(CqlExecutionProvider.class);
        classes.add(LibraryOperationsProvider.class);
        classes.add(MeasureOperationsProvider.class);
        classes.add(PlanDefinitionApplyProvider.class);

        // The plugin API will need to a way to determine whether a particular
        // service should be registered
        if(HapiProperties.getQuestionnaireResponseExtractEnabled()) { 
            classes.add(QuestionnaireProvider.class);
        };        

        if (HapiProperties.getObservationTransformEnabled()) {
            classes.add(ObservationProvider.class);
        }

        return classes;
    }

    @Bean() 
    public NarrativeProvider narrativeProvider() {
        return new NarrativeProvider();
    }

    @Bean
    public TerminologyProvider terminologyProvider(ca.uhn.fhir.jpa.term.api.ITermReadSvcR4 theTerminologySvc, ca.uhn.fhir.jpa.api.dao.DaoRegistry theDaoRegistry, ca.uhn.fhir.context.support.IValidationSupport theValidationSupport) {
        return new JpaTerminologyProvider(theTerminologySvc, theDaoRegistry, theValidationSupport);
    }
}
