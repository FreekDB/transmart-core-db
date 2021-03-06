package org.transmartproject.db.dataquery.clinical

import com.google.common.collect.Lists
import grails.test.mixin.TestMixin
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.querytool.QtQueryMaster
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.querytool.QueryResultData.createQueryResult
import static org.transmartproject.db.querytool.QueryResultData.getQueryResultFromMaster
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties

@TestMixin(RuleBasedIntegrationTestMixin)
class ClinicalDataRetrievalTests {

    ClinicalTestData testData = new ClinicalTestData()

    def clinicalDataResourceService

    def sessionFactory

    TabularResult<ClinicalVariableColumn, PatientRow> results

    @Before
    void setUp() {
        testData.saveAll()
        sessionFactory.currentSession.flush()
    }

    @After
    void tearDown() {
        results?.close()
    }

    private String getConceptCodeFor(String conceptPath) {
        testData.concepts.find {
            it.conceptCode == conceptPath
        }.conceptPath
    }

    @Test
    void testColumnsLabel() {
        results = clinicalDataResourceService.retrieveData(testData.queryResult,
                [ new TerminalConceptVariable(conceptCode: 'c2') ])

        /* label for the columns (concepts) is the concept path */
        assertThat results, hasProperty('indicesList',
                contains(allOf(
                        hasProperty('label', equalTo(
                                getConceptCodeFor('c2')
                        ))
                )))
    }

    @Test
    void testColumnIsClinicalVariable() {
        results = clinicalDataResourceService.retrieveData(testData.queryResult,
                [ new TerminalConceptVariable(conceptCode: 'c2') ])

        assertThat results, hasProperty('indicesList',
                contains(allOf(
                        isA(TerminalConceptVariable),
                        hasProperty('conceptCode', is('c2')),
                        hasProperty('conceptPath', is(
                                getConceptCodeFor('c2'))))))
    }

    @Test
    void testRowsLabel() {
        results = clinicalDataResourceService.retrieveData(testData.queryResult,
                [ new TerminalConceptVariable(conceptCode: 'c2') ])

        /* Label for the rows is the patients' inTrialId.
         * Patients are sorted by id */
        assertThat results, contains(
                testData.patients.sort {
                    it.id
                }.collect {
                    hasProperty('label', is(it.inTrialId))
                })
    }

    @Test
    void testMultipleQueryResultsVariant() {
        results = clinicalDataResourceService.retrieveData(
                testData.patients[0..1].collect {
                    QtQueryMaster result = createQueryResult([it])
                    result.save()
                    getQueryResultFromMaster(result)
                },
                [ new TerminalConceptVariable(conceptCode: 'c2') ])

        assertThat results, contains(
                testData.patients[0..1].
                        sort { it.id }.
                        collect {
                            hasProperty('patient', hasProperty('id', is(it.id)))
                        })
    }

    @Test
    void testPatientCanBeFoundInRow() {
        results = clinicalDataResourceService.retrieveData(testData.queryResult,
                [ new TerminalConceptVariable(conceptCode: 'c2') ])

        List<PatientRow> rows = Lists.newArrayList results

        assertThat rows, allOf(
                everyItem(isA(PatientRow)),
                contains(
                        testData.patients.sort {
                            it.id
                        }.collect {
                            hasProperty('patient',
                                    hasSameInterfaceProperties(Patient, it, ['assays']))
                        })
        )
    }

    @Test
    void testDataStringDataPoints() {
        results = clinicalDataResourceService.retrieveData(testData.queryResult,
                [ new TerminalConceptVariable(conceptCode: 'c2') ])

        List<PatientRow> rows = Lists.newArrayList results

        assertThat rows, contains(
                testData.facts.sort {
                    it.patientId
                }.findAll {
                    it.conceptCode == 'c2'
                }.collect {
                    contains it.textValue
                })
    }

    @Test
    void testMissingData() {
        /* test for when no data whatsoever is returned */

        results = clinicalDataResourceService.retrieveData(testData.queryResult,
                [ new TerminalConceptVariable(conceptCode: 'c4') ])

        List<PatientRow> rows = Lists.newArrayList results

        assertThat rows, contains(
                [ contains(nullValue()) ] * 3
        )
    }

    @Test
    void testMissingColumnValue() {
        /* test when a row has data for some but not all variables */
        results = clinicalDataResourceService.retrieveData(testData.queryResult,
                [ new TerminalConceptVariable(conceptCode: 'c3') ])

        List<PatientRow> rows = Lists.newArrayList results

        assertThat rows, contains(
                /* see test data */
                contains(isA(Number) /* -45.42 */),
                contains(equalTo('')),
                contains(nullValue()))
    }

    @Test
    void testNumericDataIsInNumericForm() {
        results = clinicalDataResourceService.retrieveData(testData.queryResult,
                [ new TerminalConceptVariable(conceptCode: 'c3') ])
        List<PatientRow> rows = Lists.newArrayList results

        assertThat rows, hasItem(allOf(
                /* see test data */
                hasProperty('patient',
                        hasSameInterfaceProperties(Patient,
                                testData.patients[2] /* -103 */, ['assays'])),
                /* numberValue prop in ObservationFact has scale 5 */
                contains(equalTo(-45.42000 /* big decimal */))))
    }


    @Test
    void testWithConceptSpecifiedByPath() {
        /* test when a row has data for some but not all variables */
        results = clinicalDataResourceService.retrieveData(testData.queryResult,
                [ new TerminalConceptVariable(conceptPath: '\\foo\\concept 2\\') ])

        assertThat results, contains(
                testData.facts.sort {
                    it.patientId
                }.findAll {
                    it.conceptCode == 'c2'
                }.collect {
                    contains it.textValue
                })
    }

    @Test
    void testColumnsAndDataAreInTheSpecifiedOrder() {
        def conceptVariables =  [
                new TerminalConceptVariable(conceptCode: 'd1'),
                new TerminalConceptVariable(conceptCode: 'c2') ]
        results = clinicalDataResourceService.retrieveData(
                testData.queryResult, conceptVariables)

        def expectedOrder = [ 'd1', 'c2' ]

        def createMatcher = { ->
            contains(
                    testData.facts.sort { ObservationFact fact1,
                                          ObservationFact fact2 ->
                        fact1.patientId <=> fact2.patientId ?:
                                expectedOrder.indexOf(fact1.conceptCode) <=>
                                        expectedOrder.indexOf(fact2.conceptCode)
                    }.findAll {
                        it.conceptCode == 'c2' || it.conceptCode == 'd1'
                    }.groupBy {
                        it.patientId
                    }.collect { patientId, List<ObservationFact> facts ->
                        contains(facts*.textValue.collect { is it })
                    })
        }

        assertThat Lists.newArrayList(results), createMatcher()
        results.close()

        /* now with variables reversed */
        conceptVariables = conceptVariables.reverse()

        results = clinicalDataResourceService.retrieveData(
                testData.queryResult, conceptVariables)

        expectedOrder = expectedOrder.reverse()

        assertThat Lists.newArrayList(results), createMatcher()
    }


    @Test
    void testRepeatedDataPoint() {
        ClinicalTestData.createObservationFact(
                testData.concepts.find { it.conceptCode == 'c2' },
                testData.patients[1],
                -20000,
                'foobar').save(failOnError: true)
        sessionFactory.currentSession.flush()

        def exception = shouldFail UnexpectedResultException, {
            results = clinicalDataResourceService.retrieveData(
                    testData.queryResult,
                    [ new TerminalConceptVariable(conceptCode: 'c2') ])
            def res = Lists.newArrayList results /* consume the data */
            println res
        }

        assertThat exception, hasProperty('message',
                containsString('Got more than one fact'))
    }


    @Test
    void testInexistentConcept() {

        assertThat shouldFail(InvalidArgumentsException, {
            clinicalDataResourceService.retrieveData(testData.queryResult,
                    [ new TerminalConceptVariable(conceptCode: 'non_existent') ])
        }), hasProperty('message', allOf(
                containsString('Concept code'),
                containsString('did not yield any results')))
    }

    @Test
    void testRepeatedConceptCode() {
        assertThat shouldFail(InvalidArgumentsException, {
            clinicalDataResourceService.retrieveData(testData.queryResult,
                    [ new TerminalConceptVariable(conceptCode: 'c2'),
                            new TerminalConceptVariable(conceptCode: 'c4'),
                            new TerminalConceptVariable(conceptCode: 'c2') ])
        }), hasProperty('message', containsString('same concept code'))
    }

    @Test
    void testRepeatedConceptPath() {
        assertThat shouldFail(InvalidArgumentsException, {
            clinicalDataResourceService.retrieveData(testData.queryResult,
                    [ new TerminalConceptVariable(conceptPath: getConceptCodeFor('c2')),
                            new TerminalConceptVariable(conceptPath: getConceptCodeFor('c2'))])
        }), hasProperty('message', containsString('same concept path'))
    }


    @Test
    void testMixedRepetition() {
        assertThat shouldFail(InvalidArgumentsException, {
            clinicalDataResourceService.retrieveData(testData.queryResult,
                    [ new TerminalConceptVariable(conceptCode: 'c2'),
                            new TerminalConceptVariable(conceptPath: getConceptCodeFor('c2'))])
        }), hasProperty('message', containsString('Repeated variables in the query'))
    }


}
